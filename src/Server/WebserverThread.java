package Server;

import Server.Exceptions.InvalidRequestException;
import Server.Routes.*;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * thread being created for each user.
 * @author Joscha Henningsen
 */
public class WebserverThread extends Thread {
  private TemplateProcessor tp;
  private Socket client;
  private ArrayList<Route> routes;

  /**
   * Called by the Main method each time a user connects.
   * Make sure to include every Route in this method you want to be publicly available
   * @param client
   */
  public WebserverThread(Socket client) {
    this.client = client;

    routes=new ArrayList<>();

    //important:
    routes.add(new MainPage());
    routes.add(new GetStartedPage());
  }

  /**
   * composes the http response and sends it back to the user.
   * This method basically is the heart of the server.- Make sure to be careful editing it
   * @param in
   * @param out
   * @param outputStream
   * @throws IOException
   */
  private void communicate(BufferedReader in, PrintWriter out, OutputStream outputStream) throws IOException {
    String requestLine = in.readLine();

    if (requestLine == null)
      return;
    if (requestLine.contains("?"))
      requestLine=requestLine.split("\\?")[0];
    System.out.println(requestLine);
    AtomicReference<String> otherLines= new AtomicReference<>("");
    in.lines().takeWhile(l->!l.equals("")).forEach(l->otherLines.getAndSet(otherLines.get()+"\n"+l));
    System.out.println("=> Request header received");
    HttpRequest request;
    try {
      request = new HttpRequest(requestLine, otherLines.get());
    } catch (InvalidRequestException ex) {
      System.out.println("=> Bad request!");
      out.print(new HttpResponse(HttpStatus.BadRequest));
      return;
    }

    if (request.getMethod() != HttpMethod.GET&&request.getMethod() != HttpMethod.POST) {
      System.out.println("=> Invalid method!");
      out.print(new HttpResponse(HttpStatus.MethodNotAllowed));
      return;
    }else if (request.getMethod()==HttpMethod.POST){
      //todo: handle post request 
    }

    Route response=null;
    //finds the correct route for the request
    for (Route route:routes) {
      if (route!=null){
        if ((route.acceptsSubPages()&&request.getPath().startsWith(route.getUrl()+"/"))||route.getUrl().equals(request.getPath())){
          response=route;
        }
      }
    }

    //assumes request is send to a file, if no route exists
    if (response==null) {
      new FileRequest(request.getPath(), out, outputStream);
      return;
    }else {
      response.setRequestData(request);
    }
    out.print(response.getResponse());
  }

  /**
   * Starts thread that interacts with the user
   */
  @Override
  public void run() {
    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
      PrintWriter out = new PrintWriter(new OutputStreamWriter(client.getOutputStream()));
      try {
        communicate(in, out, client.getOutputStream());
      } catch (IOException exp) {
        exp.printStackTrace();
      } finally {
        out.close();
        client.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
