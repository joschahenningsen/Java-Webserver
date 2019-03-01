package Server;

import Server.Routes.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class WebserverThread extends Thread {
  private TemplateProcessor tp;
  private Socket client;
  private ArrayList<Route> routes;
  
  public WebserverThread(TemplateProcessor tp, Socket client) {
    this.tp = tp;
    this.client = client;

    routes=new ArrayList<>();
    routes.add(new MainPage());
    routes.add(new MainCss());
  }


  private HttpResponse handleFileRequest(String fileName) {
    if (!fileName.contains("/content/"))
      return new HttpResponse(HttpStatus.Forbidden);
    String[] fileContents = Terminal.readFile(fileName);
    if (fileContents == null)
      return new HttpResponse(HttpStatus.NotFound);
    if(fileContents.length != 2)
      return new HttpResponse(HttpStatus.Forbidden);
    String body = fileContents[1];
    return new HttpResponse(HttpStatus.Ok, body);
  }

  private void communicate(BufferedReader in, PrintWriter out) throws IOException {
    String requestLine = in.readLine();
    System.out.println(requestLine);
    if (requestLine == null)
      return;

    AtomicReference<String> otherLines= new AtomicReference<>("");
    in.lines().takeWhile(l->!l.equals("")).forEach(l->otherLines.set(otherLines.get()+"\n"+l));
    System.out.println(otherLines.get());
    System.out.println("=> Request header received");
    HttpRequest request;
    try {
      request = new HttpRequest(requestLine);
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
    routes.stream().filter(route -> route.getName().equals(request.getPath())).forEach(route-> response.set(route));
    if (response.get()==null)
      response.set(new NotFoundError());
    out.print(response.get().getResponse());
  }
  
  @Override
  public void run() {
    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
      PrintWriter out = new PrintWriter(new OutputStreamWriter(client.getOutputStream()));
      try {
        communicate(in, out);
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