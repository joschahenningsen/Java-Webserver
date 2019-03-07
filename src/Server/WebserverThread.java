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
      //out.println("got header");
      //System.out.println(in.readLine());
    }

    AtomicReference<Route> response=new AtomicReference<>();
    //finds the correct route for the request
    routes.stream().filter(route->route.getUrl()!=null).filter(route -> route.getUrl().equals(request.getPath())).forEach(route-> response.set(route));

    if (response.get()==null) {
      new FileRequest(request.getPath(), out, outputStream);
      return;
    }else {
      response.get().setRequestData(request);
    }
    out.print(response.get().getResponse());
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
