package jrec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class Recorder implements ClientHttpRequestInterceptor {
  private final VCRMode mode;
  private CassetteRepository cassetteRepository;
  private Set<RecordingListener> recordingListeners;
  private String nextTest;

  @Autowired
  public Recorder(CassetteRepository cassetteRepository,
                  @Value("#{systemProperties['vcr.mode']}") String mode,
                  List<RestTemplate> restTemplates) {
    this.mode = VCRMode.valueOf(mode.toUpperCase());
    this.cassetteRepository = cassetteRepository;
    recordingListeners = new HashSet<RecordingListener>();
    JRecRuntTime.registerRecorder(this);
    for(RestTemplate restTemplate : restTemplates) restTemplate.getInterceptors().add(this);
  }

  public void addRecordingListener(RecordingListener listener) {
    recordingListeners.add(listener);
  }

  @Override
  public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    if (nextTest == null) {
      notifyRequestSkipped(request);
      return execute(request, body, execution);
    }

    ClientHttpResponse response = null;
    if (mode.playing()) response = getRecordFor(request);
    if (response == null && mode.recording()) return recordedResponseFor(request, body, execution);
    return response;
  }

  private ClientHttpResponse getRecordFor(HttpRequest request) {
    ClientHttpResponse recordedResponse = null;
    try {
      recordedResponse = cassetteRepository.responseFor(request, nextTest);
    } catch (IOException error) {
      notifyErrorReadingCassette(request, error);
    }

    if (recordedResponse == null) {
      notifyCassetteNotFound(request);
    } else {
      notifyReadingFromCassette(request, recordedResponse);
    }

    return recordedResponse;
  }

  private ClientHttpResponse recordedResponseFor(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    ClientHttpResponse response = null;
    try {
      response = execute(request, body, execution);
    } catch (IOException e) {
      notifyRequestFailed(request);
      throw e;
    }
    return recordedResponse(request, response);
  }

  private ClientHttpResponse execute(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    return execution.execute(request, body);
  }

  private ClientHttpResponse recordedResponse(HttpRequest request, ClientHttpResponse response) {
    ClientHttpResponse recordedResponse = null;
    try {
      recordedResponse = cassetteRepository.record(request, response, nextTest);
      notifyRecorded(request, recordedResponse);
    } catch (IOException e) {
      notifyFailedToCreateCassette(request, response, e);
    }
    return recordedResponse == null ? response : recordedResponse;
  }

  private void notifyRecorded(HttpRequest request, ClientHttpResponse response) {
    for (RecordingListener listener : recordingListeners) listener.recorded(request, response);
  }

  private void notifyRequestFailed(HttpRequest request) {
    for (RecordingListener listener : recordingListeners) listener.failedToExecuteRequest(request);
  }

  private void notifyErrorReadingCassette(HttpRequest request, IOException error) {
    for (RecordingListener listener : recordingListeners) listener.errorReadingCassette(request, error);
  }

  private void notifyReadingFromCassette(HttpRequest request, ClientHttpResponse response) {
    for (RecordingListener listener : recordingListeners) listener.readingFromCassette(request, response);
  }

  private void notifyCassetteNotFound(HttpRequest request) {
    for (RecordingListener listener : recordingListeners) listener.failedToFindCassette(request);
  }

  private void notifyFailedToCreateCassette(HttpRequest request, ClientHttpResponse response, IOException error) {
    for (RecordingListener listener : recordingListeners) listener.failedToCreateCassette(request, response, error);
  }

  private void notifyRequestSkipped(HttpRequest request) {
    for (RecordingListener listener : recordingListeners) listener.requestSkipped(request);
  }

  public void setNextTest(String testName) {
    this.nextTest = testName;
  }
}
