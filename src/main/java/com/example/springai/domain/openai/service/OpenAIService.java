package com.example.springai.domain.openai.service;

import com.openai.models.audio.AudioResponseFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.audio.tts.Speech;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.*;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OpenAIService {

    // Chat,Embedding,Image,Audio(STT,TTS) model 필드 생성
    private final OpenAiChatModel openAiChatModel;
    private final OpenAiEmbeddingModel openAiEmbeddingModel;
    private final OpenAiImageModel openAiImageModel;
    private final OpenAiAudioSpeechModel openAiAudioSpeechModel;
    private final OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel;

    /** 1-1. ChatModel : response
     * 호출되면 모든 응답이 생성된 이후에 전체응답을 받는 메소드 */
    public String generate(String text) {

        // 메세지
        SystemMessage systemMessage = new SystemMessage("");
        UserMessage userMessage = new UserMessage(text);
        AssistantMessage assistantMessage = new AssistantMessage("");

        // 옵션
        /* 설정 커스텀은 Introduction :: SpringAI Reference -> Reference -> Models
        -> Chat Models -> OpenAI 해당 경로에 들어온 후 개인적으로 필요한 커스텀 설정진행*/
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model("gpt-4o-mini")
                .temperature(0.7)
                .build();

        // 프롬프트
        // List.of를 이용해 새로운 배열생성 후 위 설정한 메세지(system, user, assistant) 삽입
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage, assistantMessage), options);


        // 요청 및 응답
        ChatResponse response = openAiChatModel.call(prompt);

        return response.getResult().getOutput().getText();
    }

    /** 1-2. ChatModel : response stream
    String으로 text를 보낸 이후에 토큰 단위로 생성되는
    즉시 stream한 형태로 받을 수 있는 메소드
     */
    public Flux<String> generateStream(String text) {

        // 메세지
        SystemMessage systemMessage = new SystemMessage("");
        UserMessage userMessage = new UserMessage(text);
        AssistantMessage assistantMessage = new AssistantMessage("");

        // 옵션(위 1-1 메소드와 동일하게 공식사이트 접속 후 원하는 커스텀 설정)
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model("gpt-4o-mini")
                .temperature(0.7)
                .build();

        // 프롬프트
        // List.of로 새로운 리스트 생성 후 안에 설정한 메세지(system, user, assistant)삽입
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage, assistantMessage), options);

        return openAiChatModel.stream(prompt)
                .mapNotNull(response -> response.getResult().getOutput().getText());
    }

    /** 2. Embedding API 호출 메소드
     * 사용자가 텍스트를 보내면 해당 텍스트에 대해서
     * Embedding 벡터를 보낼 수 있는 메소드*/

    public List<float[]> generateEmbedding(List<String> texts, String model) {

        // 옵션
        // 어떤 모델을 사용할 건지에 대해서만 세팅
        EmbeddingOptions embeddingOptions = OpenAiEmbeddingOptions.builder()
                .model(model).build();

        // 프롬프트
        // 사용자한테 받은 문장 texts, 위 설정한 옵션(모델)을 EmbeddingRequest로 프롬프트 설정
        EmbeddingRequest prompt = new EmbeddingRequest(texts, embeddingOptions);


        // 요청 및 응답
        /* 1. 위 의존성 주입받은 openAiEmbeddingMoedel한테 call 메소드를 통해서 설정한 프롬프트를 요청
           2. 요청 후 받은 응답(response)을 stream하게 map함수로 새로운 배열로 하나하나 끄집어 내서 list로 반환
         */
        EmbeddingResponse response = openAiEmbeddingModel.call(prompt);
        return response.getResults().stream()
                .map(Embedding::getOutput)
                .toList();
    }

    /** 3. Image Model API 호출메소드
     * */
    public List<String> generateImages(String text, int count, int height, int width) {

        // 옵션
        /*quality : 이미지 퀄리티 설정
        * n : 이미지 갯수 설정
        * height : 이미지 높이 규격 설정
        * width : 이미지 두께 규격 설정*/
        OpenAiImageOptions imageOptions = OpenAiImageOptions.builder()
                .quality("hd")
                .n(count)
                .height(height)
                .width(width)
                .build();

        // 프롬프트
        // 사용자의 이미지 텍스트 프롬프트, 옵션값을 주입하여 최종 프롬프트 완성
        ImagePrompt prompt = new ImagePrompt(text, imageOptions);

        // 요청
        // 의존성 주입받은 openAiImageModel의 call메소드로 최종 프롬프트 삽입 후 요청
        ImageResponse response = openAiImageModel.call(prompt);

        //응답(요청을 보내면 OpenAI서버에 올라가있는 이미지에 대한 URL주소를 리스트 문자열 형태로 응답 받음)
        return response.getResults().stream()
                .map(image -> image.getOutput().getUrl())
                .toList();
    }

    /** 4-1. Audio(TTS) Model API 호출 메소드*/
    public byte[] tts(String text) {

        // 옵션
        OpenAiAudioSpeechOptions speechOptions = OpenAiAudioSpeechOptions.builder()
                .responseFormat("mp3") //mp3형식으로 데이터 받음
                .speed(1.0) //정배속 세팅
                .model("tts-1") // 모델 설정
                .build();

        // 프롬프트(사용자의 텍스트, 설정한 옵션을 삽입해 프롬프트 설정)
        TextToSpeechPrompt prompt = new TextToSpeechPrompt(text, speechOptions);

        // 요청(의존성 주입한 openAiAudioSpeechMoedel안 call메소드를 이용해 프롬프트 삽입 후 요청)
        TextToSpeechResponse response = openAiAudioSpeechModel.call(prompt);

        // 응답
        return response.getResult().getOutput();
    }

    /** 4-2. Audio(STT) Model API 호출 메소드*/
    public String stt(Resource audioFile) {

        // 옵션
        OpenAiAudioTranscriptionOptions transcriptionOptions = OpenAiAudioTranscriptionOptions.builder()
                .language("ko") // 오디오파일 언어는 한국어로 되어있다 명시
                .prompt("Ask not this, but ask that") // 음성 인식 전 참고할 텍스트 프롬프트
                .temperature(0f)
                .model("whisper-1") // STT는 무조건 whisper-1 모델을 써야함
                .responseFormat(AudioResponseFormat.VTT) // 응답할 포맷 VTT 자막형식
                .build();

        // 프롬프트(사용자에게 받은 리소스 오디오파일, 위 설정한 옵션을 삽입해 프롬프트 설정)
        AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(audioFile, transcriptionOptions);

        // 요청(의존성 주입한 openAiAudioTranscriptionModel안 call메소드를 이용해 프롬프트 삽입 후 요청)
        AudioTranscriptionResponse response = openAiAudioTranscriptionModel.call(prompt);

        // 응답
        return response.getResult().getOutput();
    }
}
