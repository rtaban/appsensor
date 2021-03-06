package org.owasp.appsensor.handler;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.jws.HandlerChain;
import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import org.owasp.appsensor.AppSensorServer;
import org.owasp.appsensor.Attack;
import org.owasp.appsensor.ClientApplication;
import org.owasp.appsensor.Event;
import org.owasp.appsensor.Response;
import org.owasp.appsensor.accesscontrol.Action;
import org.owasp.appsensor.criteria.SearchCriteria;
import org.owasp.appsensor.exceptions.NotAuthorizedException;
import org.owasp.appsensor.util.StringUtils;

/**
 * This is the soap endpoint that handles requests on the server-side. 
 * 
 * @author John Melton (jtmelton@gmail.com) http://www.jtmelton.com/
 */
@WebService(
        portName = "SoapRequestHandlerPort",
        serviceName = "SoapRequestHandlerService",
        targetNamespace = "https://www.owasp.org/index.php/OWASP_AppSensor_Project/wsdl",
        endpointInterface = "org.owasp.appsensor.handler.SoapRequestHandler")
@HandlerChain(file="handler-chain.xml")
public class ReferenceSoapRequestHandler implements SoapRequestHandler {

	@Resource 
	private WebServiceContext wsContext;
	
	/**
	 * {@inheritDoc}
	 */
	@WebMethod
	@Override
	public void addEvent(Event event) throws NotAuthorizedException {
		checkAuthorization(Action.ADD_EVENT);
		
		event.setDetectionSystemId(getClientApplicationName());
		
		AppSensorServer.getInstance().getEventStore().addEvent(event);
	}

	/**
	 * {@inheritDoc}
	 */
	@WebMethod
	@Override
	public void addAttack(Attack attack) throws NotAuthorizedException {
		checkAuthorization(Action.ADD_ATTACK);
		
		attack.setDetectionSystemId(getClientApplicationName());
		
		AppSensorServer.getInstance().getAttackStore().addAttack(attack);
	}

	/**
	 * {@inheritDoc}
	 */
	@WebMethod
	@Override
	public Collection<Response> getResponses(Long earliest) throws NotAuthorizedException {
		checkAuthorization(Action.GET_RESPONSES);
		
		SearchCriteria criteria = new SearchCriteria().
				setDetectionSystemIds(StringUtils.toCollection(getClientApplicationName())).
				setEarliest(earliest);
		
		return AppSensorServer.getInstance().getResponseStore().findResponses(criteria);
	}
	
	/**
	 * Check authz before performing action.
	 * @param action desired action
	 * @throws NotAuthorizedException thrown if user does not have role.
	 */
	private void checkAuthorization(Action action) throws NotAuthorizedException {
		@SuppressWarnings("unchecked")
		Map<String, List<String>> httpHeaders = (Map<String, List<String>>) wsContext.getMessageContext().get(MessageContext.HTTP_REQUEST_HEADERS);
		String clientApplicationName = httpHeaders.get(APPSENSOR_CLIENT_APPLICATION_IDENTIFIER_ATTR).get(0);

		ClientApplication clientApplication = AppSensorServer.getInstance().getConfiguration().findClientApplication(clientApplicationName);
		
		org.owasp.appsensor.accesscontrol.Context context = new org.owasp.appsensor.accesscontrol.Context();
		
		AppSensorServer.getInstance().getAccessController().assertAuthorized(clientApplication, action, context);
	}
	
	private String getClientApplicationName() {
		@SuppressWarnings("unchecked")
		Map<String, List<String>> httpHeaders = (Map<String, List<String>>) wsContext.getMessageContext().get(MessageContext.HTTP_REQUEST_HEADERS);
		
		String clientApplicationName = httpHeaders.get(APPSENSOR_CLIENT_APPLICATION_IDENTIFIER_ATTR).get(0);
		
		return clientApplicationName;
	}

}
