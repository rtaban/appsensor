package org.owasp.appsensor.reporting;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Observable;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.owasp.appsensor.AppSensorServer;
import org.owasp.appsensor.Attack;
import org.owasp.appsensor.Event;
import org.owasp.appsensor.Response;
import org.owasp.appsensor.logging.Logger;

import com.google.gson.Gson;

@ClientEndpoint
public class WebSocketReportingEngine implements ReportingEngine {
	
	private Session localSession = null;
	
	private static Logger logger = AppSensorServer.getInstance().getLogger().setLoggerClass(WebSocketReportingEngine.class);
	
	private boolean webSocketInitialized = false;
	
	private Gson gson = new Gson();
	
	public WebSocketReportingEngine() {
	}
	
	@Override
	public void update(Observable observable, Object observedObject) {
		ensureConnected();
		if (observedObject instanceof Event) {
			Event event = (Event)observedObject;
			
			notifyWebSocket("event", event);
			
			logger.info("Reporter observed event by user [" + event.getUser().getUsername() + "]");
		} else if (observedObject instanceof Attack) {
			Attack attack = (Attack)observedObject;

			notifyWebSocket("attack", attack);
			
			logger.info("Reporter observed attack by user [" + attack.getUser().getUsername() + "]");
		} else if (observedObject instanceof Response) {
			Response response = (Response)observedObject;

			notifyWebSocket("response", response);
			
			logger.info("Reporter observed response for user [" + response.getUser().getUsername() + "]");
		}
	}

	@Override
	public Collection<Event> findEvents(Long earliest) {
		throw new UnsupportedOperationException("This method is not implemented for WebSocket reporting implementation");
	}

	@Override
	public Collection<Attack> findAttacks(Long earliest) {
		throw new UnsupportedOperationException("This method is not implemented for WebSocket reporting implementation");
	}

	@Override
	public Collection<Response> findResponses(Long earliest) {
		throw new UnsupportedOperationException("This method is not implemented for WebSocket reporting implementation");
	}
	
	private void notifyWebSocket(String type, Object object) {
		if (localSession != null && localSession.isOpen()) {
			try {
				WebSocketJsonObject jsonObject = new WebSocketJsonObject(type, object);
				String json = gson.toJson(jsonObject);
				localSession.getBasicRemote().sendText(json);
			} catch (IOException e) {
				logger.error("Error sending data to websocket", e);
			}
		}
	}
	
	@OnOpen
	public void onOpen(Session session) {
		logger.info("Connected ... " + session.getId());
	}

	@OnMessage
	public String onMessage(String message, Session session) {
		return null;
	}

	@OnClose
	public void onClose(Session session, CloseReason closeReason) {
		logger.info(String.format("Session %s close because of %s",
				session.getId(), closeReason));
	}

	private void ensureConnected() {
		if (! webSocketInitialized) {
			WebSocketContainer client = ContainerProvider.getWebSocketContainer();
	
			try {
	            localSession = client.connectToServer(WebSocketReportingEngine.class, new URI("ws://localhost:8080/simple-websocket-dashboard/dashboard"));
	            webSocketInitialized = true;
	        } catch (DeploymentException | URISyntaxException | IOException e) {
	            throw new RuntimeException(e);
	        }
	    	System.err.println("started and connected");
		}
	}
    
}
