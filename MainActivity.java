package sg.gowild.sademo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ai.api.AIConfiguration;
import ai.api.AIDataService;
import ai.api.AIServiceException;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Fulfillment;
import ai.api.model.Result;
import ai.kitt.snowboy.SnowboyDetect;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    // View Variables
    private Button button;
    private TextView textView;
    //public TextView visualCount;
    //public TextView auditoryCount;
    //public TextView handsonCount;

    // ASR Variables
    private SpeechRecognizer speechRecognizer;

    // TTS Variables
    private TextToSpeech textToSpeech;

    // NLU Variables
    private AIDataService aiDataService;

    // Hotword Variables
    private boolean shouldDetect;
    private SnowboyDetect snowboyDetect;

    //Check if learning style assessment is being requested
    public boolean startAssessing = false;

    //Observation findings
    public String[] observe = {"Likes reading books",
            "Learns better when seeing study topic written out or shown",
            "Distracted by some objects of interest when studying and thus lose concentration easily"};

    //Counter variables
    public int visual = 0;
    public int auditory = 0;
    public int handsOn = 0;

    //Learning type related answers dictionary
    protected String[] visualRelated = {"picture", "pictures", "look", "looks", "watch", "watches",
            "visual", "visuals", "observe", "observes", "observant", "by reading", "likes reading",
            "by its appearance", "seeing", "pays attention to details"};

    protected String[] auditoryRelated = {"auditory", "listen", "listens", "listening", "hearing", "hears", "heard",
            "by its name", "cannot concentrate in a noisy environment", "talkative",
            "lively", "cannot concentrate", "lose concentration easily"};

    protected String[] handsOnRelated = {"hands-on", "hands on", "likes to try new things", "hands on activity",
            "hands-on activity", "outdoor person", "likes to try something new", "Hands-On activity"};

    //Reserved questions or statements
    public CharSequence visualLearning = "What is visual learning?";
    public CharSequence auditoryLearning = "What is auditory learning?";
    public CharSequence handsOnLearning = "What is hands on learning?";
    public CharSequence assessRequestQn = "Alright, please answer the following questions. Question 1, " +
            "How does your child remember things? By name or by its appearance?";
    public CharSequence assessEnd = "I will determine the results of this assessment";

    //Keep record of learning style of child
    public ArrayList<String> records = new ArrayList<String>();

    static {
        System.loadLibrary("snowboy-detect-android");
    }

    /* (For later use)
    public void addToRecords (String result) {
        CharSequence visualLearner = "visual learner";
        CharSequence auditoryLearner = "auditory learner";
        CharSequence handsOnLearner = "hands on learner";

        if (result.contains(visualLearner)) {
            records.add("Visual Learner");
        }
        else if (result.contains(auditoryLearner)) {
            records.add("Auditory Learner");
        }
        else if (result.contains(handsOnLearner)) {
            records.add("Hands-on learner");
        }
    }
    */

    public void checkTestStart(String aiResponse){
       if (aiResponse.contains(assessRequestQn)) {
           startAssessing = true;

           //Calculate scores from observation findings
           for (int z = 0; z < observe.length; z++) {
               String observation = observe[z].toLowerCase();

               for (int a = 0; a < visualRelated.length; a++) {
                   CharSequence word = visualRelated[a];
                   if (observation.contains(word)) {
                       visual++;
                   }
               }
               for (int b = 0; b < auditoryRelated.length; b++) {
                   CharSequence word = auditoryRelated[b];
                   if (observation.contains(word)) {
                       auditory++;
                   }
               }
               for (int c = 0; c < handsOnRelated.length; c++) {
                   CharSequence word = handsOnRelated[c];
                   if (observation.contains(word)) {
                       handsOn++;
                   }
               }
           }
       }
    }

    //Count number of words related to learning style characteristics
    public void assessmentStart(String userResponse) {
        if (startAssessing == true) {
            //This if is a safety net incase startAssessing is the wrong value
            if (userResponse.contains(visualLearning) == false && userResponse.contains(auditoryLearning) == false
                    && userResponse.contains(handsOnLearning) == false) {
                //Find characteristics of learning style from sentence
                //This for loop is for visual learning
                for (int a = 0; a < visualRelated.length; a++) {
                    CharSequence word = visualRelated[a];
                    if (userResponse.contains(word)) {
                        visual++;
                    }
                }
                //This for loop is for auditory learning
                for (int b = 0; b < auditoryRelated.length; b++) {
                    CharSequence word = auditoryRelated[b];
                    if (userResponse.contains(word)) {
                        auditory++;
                    }
                }
                //This for loop is for hands on learning
                for (int c = 0; c < handsOnRelated.length; c++) {
                    CharSequence word = handsOnRelated[c];
                    if (userResponse.contains(word)) {
                        handsOn++;
                    }
                }
                //Test code below
                //visualCount.setText(String.valueOf(visual));
                //auditoryCount.setText(String.valueOf(auditory));
                //handsonCount.setText(String.valueOf(handsOn));
            }
        }
    }

    public void assessmentResult() {

        int largest = Integer.MIN_VALUE;
        ;
        int secondLargest = Integer.MIN_VALUE;
        ;
        int thirdLargest = Integer.MIN_VALUE;
        ;
        int highestPos = 0;

        String result = "";

        //Result array
        int[] results = new int[3];
        results[0] = visual;
        results[1] = auditory;
        results[2] = handsOn;

        for (int i = 0; i < results.length; i++) {

            //Nested if finds highest scoring component out of the 3 counter variables
            if (largest < results[i]) {
                thirdLargest = secondLargest;
                secondLargest = largest;
                largest = results[i];
                highestPos = i;
            } else if ((secondLargest < results[i]) && (results[i] != largest)) {
                thirdLargest = secondLargest;
                secondLargest = results[i];
            } else if ((thirdLargest < results[i]) && (results[i] != secondLargest)) {
                thirdLargest = results[i];
            }

        }

        if (highestPos == 0) {
            result = "Your child is a visual learner";
        } else if (highestPos == 1) {
            result = "Your child is a auditory learner";
        } else {
            result = "Your child is a hands on learner";
        }

        startAssessing = false;

        //AI announces result
        startTts(result);

        //Reset values for next usage
        visual = 0;
        auditory = 0;
        handsOn = 0;

        //Records this test's result into arraylist (For later use)
        //addToRecords(result);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TODO: Setup Components
        setupViews();
        setupXiaoBaiButton();
        setupAsr();
        setupTts();
        setupNlu();
        setupHotword();
        // TODO: Start Hotword
        startHotword();
    }

    private void setupViews() {
        // TODO: Setup Views
        textView = findViewById(R.id.textview);
        //visualCount = findViewById(R.id.visualcount);
        //auditoryCount = findViewById(R.id.auditorycount);
        //handsonCount = findViewById(R.id.handsoncount);
        button = findViewById(R.id.button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //textView.setText("Good Morning");
                shouldDetect = false;
            }
        });
    }

    private void setupXiaoBaiButton() {
        String BUTTON_ACTION = "com.gowild.action.clickDown_action";

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BUTTON_ACTION);

        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // TODO: Add action to do after button press is detected
                shouldDetect = false;
            }
        };
        registerReceiver(broadcastReceiver, intentFilter);
    }

    private void setupAsr() {
        // TODO: Setup ASR
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                String text = "I'm ready to listen.";
                textView.setText(text);
            }

            @Override //start to speak
            public void onBeginningOfSpeech() {
                String text = "-Speaking-";
                textView.setText(text);
            }

            @Override //volume of voice
            public void onRmsChanged(float rmsdB) {

            }

            @Override //when robot receives voice
            public void onBufferReceived(byte[] buffer) {
                textView.setText("-Listening-");
            }

            @Override //detects when we stop speaking
            public void onEndOfSpeech() {
                String text = "-User not speaking-";
                textView.setText(text);
            }

            @Override
            public void onError(int error) {
                Log.e( "asr","error: " + Integer.toString(error));
                startHotword();
            }

            @Override
            public void onResults(Bundle results) {
                List<String> texts = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                if (texts == null || texts.isEmpty()){
                    textView.setText("Please try again");
                }

                else {
                    String text = texts.get(0);
                    textView.setText(text);
                    //Assessment starts when startAssessing is true
                    assessmentStart(text);
                    startNlu(text);
                }
            }

            //Checks whether you have stopped based on sentences
            @Override
            public void onPartialResults(Bundle partialResults) {

            }

            @Override
            public void onEvent(int eventType, Bundle params) {
                String text = "-Event executing-";
                textView.setText(text);
            }
        });
    }

    private void startAsr() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                // TODO: Set Language
                final Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en");
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en");
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);

                // Stop hotword detection in case it is still running
                shouldDetect = false;

                // TODO: Start ASR
                speechRecognizer.startListening(recognizerIntent);
            }
        };
        Threadings.runInMainThread(this, runnable);
    }

    private void setupTts() {
        // TODO: Setup TTS
        textToSpeech = new TextToSpeech(this, null);
    }

    private void startTts(String text) {
        // TODO: Start TTS
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        // TODO: Wait for end and start hotword
        // if in assessment, get user response without listening for hotword
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (textToSpeech.isSpeaking()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Log.e("tts", e.getMessage(), e);
                    }
                }
               if (startAssessing == true) {
                    startAsr();
                }
                else {
                    startHotword();
                }
            }
        };
        Threadings.runInBackgroundThread(runnable);
    }

    private void setupNlu() {
        // TODO: Change Client Access Token
        String clientAccessToken = "060610fc227442cb9d370f7e39a78947";
        AIConfiguration aiConfiguration = new AIConfiguration(clientAccessToken,
                AIConfiguration.SupportedLanguages.English);
        aiDataService = new AIDataService(aiConfiguration);
    }

    private void startNlu(final String text) {
        // TODO: Start NLU
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                AIRequest aiRequest = new AIRequest();
                aiRequest.setQuery(text);

                try {
                    AIResponse aiResponse = aiDataService.request(aiRequest);

                    Result result = aiResponse.getResult();
                    Fulfillment fulfillment = result.getFulfillment();
                    String speech = fulfillment.getSpeech();
                    if (startAssessing == false) {
                        //Check if ai has started assessment
                        checkTestStart(speech);
                        startTts(speech);
                    }
                    else if (startAssessing == true) {
                        //ai will announce result if it finishes assessment dialogue
                        if (speech.contains(assessEnd)) {
                            assessmentResult();
                        }
                        else {
                            startTts(speech);
                        }
                    }
                } catch (AIServiceException e) {
                    e.printStackTrace();
                }
            }
        };
        Threadings.runInBackgroundThread(runnable);
    }

    private void setupHotword() {
        shouldDetect = false;
        SnowboyUtils.copyAssets(this);

        // TODO: Setup Model File
        File snowboyDirectory = SnowboyUtils.getSnowboyDirectory();
        File model = new File(snowboyDirectory, "Christina.pmdl");
        File common = new File(snowboyDirectory, "common.res");

        // TODO: Set Sensitivity
        snowboyDetect = new SnowboyDetect(common.getAbsolutePath(), model.getAbsolutePath());
        snowboyDetect.setSensitivity("0.60");
        snowboyDetect.applyFrontend(true);
    }

    private void startHotword() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                shouldDetect = true;
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

                int bufferSize = 3200;
                byte[] audioBuffer = new byte[bufferSize];
                AudioRecord audioRecord = new AudioRecord(
                        MediaRecorder.AudioSource.DEFAULT,
                        16000,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize
                );

                if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                    Log.e("hotword", "audio record fail to initialize");
                    return;
                }

                audioRecord.startRecording();
                Log.d("hotword", "start listening to hotword");

                while (shouldDetect) {
                    audioRecord.read(audioBuffer, 0, audioBuffer.length);

                    short[] shortArray = new short[audioBuffer.length / 2];
                    ByteBuffer.wrap(audioBuffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortArray);

                    int result = snowboyDetect.runDetection(shortArray, shortArray.length);
                    if (result > 0) {
                        Log.d("hotword", "detected");
                        shouldDetect = false;
                    }
                }

                audioRecord.stop();
                audioRecord.release();
                Log.d("hotword", "stop listening to hotword");

                // TODO: Add action after hotword is detected
                startAsr();
            }
        };
        Threadings.runInBackgroundThread(runnable);
    }

    private String getWeather() {
        // TODO: (Optional) Get Weather Data via REST API
        return "No weather info";
    }
}
