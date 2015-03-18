package com.mteng.chatServlet;
// ChatServlet.java
// Andrew Davison, April 2005, ad@fivedots.coe.psu.ac.th

/* A chat server in servlet form.
   It accepts messages from the client as arguments to its
   URL (i.e. GET method arguments.

  Client Messages:
    * ChatServlet?cmd=hi&name=??         
        // hi message to ask to join the chat group. The
           server returns a uid cookie, or rejects the client.

    * ChatServlet?cmd=bye&name=?? + uid cookie
        // bye message to signal client departure

    * ChatServlet?cmd=who
       // no name or cookie required; returns a list of who
          is logged on currently

    * ChatServlet?cmd=msg&name=??&msg=?? + uid cookie
        // add a message to the server's list

    * ChatServlet?cmd=read&name=??  + uid cookie
        // this retrieves all the visible messages stored on
           the server since the last read

  Most messages require a user name and a UID to identify 
  the client. The UID is allocated to the client when he
  sends a "hi" message.

  Server-side infomation includes details about each client
  (name, uid, and number of messages read), and a list of
  chat messages. The information is stored in a ChatGroup object.
*/

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;


public class ChatServlet extends HttpServlet 
{
   private ChatGroup cg;   // for storing client information

   public void init() throws ServletException
   {  cg = new ChatGroup();  }


   public void doGet( HttpServletRequest request,
                      HttpServletResponse response )
      throws ServletException, IOException
   // look at the cmd parameter to decide which message the client sent
   {
     String command = request.getParameter("cmd");
     System.out.println("Command: " + command);

     if (command.equals("hi"))
       processHi(request, response);
     else if (command.equals("bye"))
       processBye(request, response);
     else if (command.equals("who"))
       processWho(response);
     else if (command.equals("msg"))
       processMsg(request, response);
     else if (command.equals("read"))
       processRead(request, response);
     else
       System.out.println("Did not understand command: " + command);
   }  // end of doGet()



  private void processHi(HttpServletRequest request,
                         HttpServletResponse response)
    throws IOException
  /* Message format: ChatServlet?cmd=hi&name=??  
       
     The client wants to join the chat group. 
     The server returns a uid cookie, or rejects the client.
  */
  {
    int uid = -1;  // default for failure
    String userName = request.getParameter("name");

    if (userName != null)
      uid = cg.addUser(userName);  // attempt to add to group

    if (uid != -1) {  // the request has been accepted
      Cookie c = new Cookie("uid", ""+uid);
      response.addCookie(c);
    }
      
    PrintWriter output = response.getWriter();
    if (uid != -1)
      output.println("ok");
    else
      output.println("no");  // request was rejected
    output.close();
  }  // end of processHi()



  private void processBye(HttpServletRequest request,
                         HttpServletResponse response)
    throws IOException
  /* Message format: ChatServlet?cmd=bye&name=?? + uid cookie

    The client is departing. use the name and cookie to delete
    their details.
  */
  {
    boolean isDeleted = false;   // default for failure
    String userName = request.getParameter("name");

    if (userName != null) {
      int uid = getUidFromCookie(request);
      isDeleted = cg.delUser(userName, uid);
    }

    PrintWriter output = response.getWriter();
    if (isDeleted)
      output.println("ok");
    else
      output.println("no");   // deletion went wrong
    output.close();
  }  // end of processBye()



  private int getUidFromCookie(HttpServletRequest request)
  // return uid value from cookie, or -1
  {
    Cookie[] cookies = request.getCookies();
    Cookie c;
    for(int i=0; i < cookies.length; i++) {
      c = cookies[i];
      if (c.getName().equals("uid")) {
        try {
          return Integer.parseInt( c.getValue() ); 
        }
        catch (Exception ex){ 
          System.out.println(ex);
          return -1;
        } 
      }
    } 
    return -1;
  } // end of getUidFromCookie()



  private void processWho(HttpServletResponse response)
    throws IOException
  /* Message format: ChatServlet?cmd=who
     Return a list of who is logged on currently.
     There is no need for the client to send a name or cookie.
  */
  { PrintWriter output = response.getWriter();
    output.print( cg.who() );    // already has a '\n'
    output.close();
  }  // end of processWho()



  private void processMsg(HttpServletRequest request,
                         HttpServletResponse response)
    throws IOException
  /* Message format: 
       ChatServlet?cmd=msg&name=??&msg=?? + uid cookie
     Add the message to the server's list
  */
  {
    boolean isStored = false;   // default for failure
    String userName = request.getParameter("name");
    String msg = request.getParameter("msg");

    System.out.println("msg: " + msg);

    if ((userName != null) && (msg != null)) {
      int uid = getUidFromCookie(request);
      isStored = cg.storeMessage(userName, uid, msg);  // add message to list
    }

    PrintWriter output = response.getWriter();
    if (isStored)
      output.println("ok");
    else
      output.println("no");   // something wrong
    output.close();
  }  // end of processBye()



  private void processRead(HttpServletRequest request,
                         HttpServletResponse response)
    throws IOException
  /* Message format: ChatServlet?cmd=read&name=??  + uid cookie

     All the visible messages stored by the server since 
     the last read by this user are sent back to him.
  */
  { int uid = -1;   // default for failure
    String userName = request.getParameter("name");

    if (userName != null)
      uid = getUidFromCookie(request);

    PrintWriter output = response.getWriter();
    if (uid != -1) {
      output.print( cg.read(userName, uid) );   // already has a '\n'
      output.flush();
    }
    else
      output.println("no");   // something wrong
    output.close();
  }  // end of processRead()


} // end of ChatServlet class

