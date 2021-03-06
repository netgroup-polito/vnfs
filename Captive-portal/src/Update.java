
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONObject;

/**
 * 
 * Servlet implementation class Update
 */
public class Update extends HttpServlet {
	String orchestrator_port;
	String orchestrator_ip;
	ServletContext context;
	
	private static final long serialVersionUID = 1L;
	/**
	 * 
	 * @see HttpServlet#HttpServlet()
	 */
	public Update() {
		super();
	}

	/**
	 * 
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException
	{
		/*
		 * This function is called by the js that handle the resources progress (jumping frog).
		 * The function calls the orchestrator asking for the percentage of the deployed resources.
		 */
		response.setContentType("text/xml");
		response.setHeader("Cache-Control", "no-cache");
		String status = getOrchestratorStatus(response, request);
		System.out.println("Update: " + status);
		response.getWriter().write(status);
	}

	private String getOrchestratorStatus(HttpServletResponse response, HttpServletRequest request)
	{
		/*
		 *  Implement the requests to the orchestrator. 
		 *  This requests needs the keystone token, of the user.
		 */
		
		try 
		{
			HttpSession session = request.getSession();
	    	String req_URI = (String) session.getAttribute("requested_path");
	    	if(req_URI != null)
	    		System.out.println("req_URI: "+ req_URI.toString());
	    	String instantiation_complete = (String) session.getAttribute("instantiation_complete");
	    	if (instantiation_complete != null)
	    		instantiation_complete = instantiation_complete.toString();
	    	String token = (String) session.getAttribute("Keystone_token");

	    	
	    	String orchestrator_ip = (String) session.getServletContext().getAttribute("orchestrator_ip");
	    	String orchestrator_port = (String) session.getServletContext().getAttribute("orchestrator_port");
	    	String orchestrator_servicepath = (String) session.getServletContext().getAttribute("orchestrator_servicepath");
			HttpClient httpClient = HttpClientBuilder.create().build(); 

			URL temp = new URL("http", orchestrator_ip, Integer.parseInt(orchestrator_port), orchestrator_servicepath);

			HttpGet putRequest = new HttpGet(temp.toString());

			putRequest.setHeader("Accept", "application/json");

			putRequest.setHeader("X-Auth-Token", token);
			
			HttpResponse orchestrator_response = httpClient.execute(putRequest);

			System.out.println("Orchestrator response:  "+orchestrator_response.getStatusLine().toString());
			int orch_response_status_code = orchestrator_response.getStatusLine().getStatusCode();
			
			/*
			 * 201 means that, the service graph is complete.
			 * 202 means that, there are some resources left until the service graph is complete.
			 */
			if ((orch_response_status_code == 201) || (orch_response_status_code == 202))
			{
				BufferedReader br = new BufferedReader(
						new InputStreamReader((orchestrator_response.getEntity().getContent())));

				String json_response = br.readLine();
			   	if (json_response != null)
			   	{
			   		JSONObject jsonObject = new JSONObject(json_response);
			   		jsonObject.append("requested_URI", req_URI);
			   		if(instantiation_complete != null){
			   			jsonObject.append("instantiation_complete", instantiation_complete);
			   			System.out.println("instantiation_complete setted");
			   		} else {
			   			jsonObject.append("instantiation_complete", false);
			   		}
			   			
			   		String res = jsonObject.toString();
					if (orch_response_status_code == 201)
					{	
						/*
						 * Orchestrator response confirm the successful instantiation
						 * of the user profile.
						 */
						Boolean status = true;
						session.setAttribute("instantiation_complete", status.toString());
						response.setHeader("Connection","close");
					}
			   		if (res != null)
			   			return res;
			   	}
			} else
			{

				response.setStatus(orchestrator_response.getStatusLine().getStatusCode());
				return "Some error occurs";
			}

		  } catch (ClientProtocolException e) {

				System.err.println("HTTP Put Error: "+e.getMessage());
				throw new RuntimeException("We encounter an unhandable problem in the request processing. Contact the system administrator.");

		  } catch (IOException e) {

				System.err.println(e.getMessage());
				throw new RuntimeException("We encounter an unhandable problem in the request processing. Contact the system administrator.");
		  }
		  response.setStatus(500);
		  return "Some error occurs";
	}
}
