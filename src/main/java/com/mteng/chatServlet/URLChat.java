package com.mteng.chatServlet;
// URLChat.java
// Andrew Davison, April 2005, ad@fivedots.coe.psu.ac.th
/*
  A Chat client that uses a servlet as the server. Messages 
  are sent as arguments to the servlet's URL.
  Responses are text based (often just "ok" or "no").

  The client obtains a cookie when it first connects, and
  uses that in subsequent communication (along with the client's
  name) as a simple way of confirming its identity. 
  The cookie contains a user ID (uid).

  Client Messages:
    * ChatServlet?cmd=hi&name=??         
        // hi message to ask to join the chat group. The
           server returns a uid cookie, or rejects the client.

    * ChatServlet?cmd=bye&name=?? + uid cookie
        // bye message to signal client departure

    * ChatServlet?cmd=who
       // the "who" message; no name or cookie required.
          Returns a list of who is logged on currently

    * ChatServlet?cmd=msg&name=??&msg=?? + uid cookie
        // add a message to the server's chat messages list


  There is a separate thread, URLChatWatcher which periodically
  sends a "read" message to the servlet:
      ChatServlet?cmd=read&name=??  + uid cookie
        // this retrieves all the visible messages stored on
           the server since the last read

  Info about URLs and Cookies on the client-side:
    http://chantal.nobilitas.com/~martin/java/cookies.html
    http://javaalmanac.com/egs/java.net/GetCookies.html


  ---- Changes: 30 August 2004 ---

  Modified showMsg() to move the caret position, and use invokeLater()
  to avoid Swing+threads problem. For details see:
  http://java.sun.com/products/jfc/tsc/articles/threads/threads1.html
    - thanks to Rachel Struthers (rmstruthers@mn.rr.com)

  Moved new URLChatWatcher() call to after window made visible.

  Server port changed to 8100 from 8080.
*/

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;


public class URLChat extends JFrame implements ActionListener
{
//  private static final String SERVER = "http://localhost:8100/servlet/ChatServlet";
    private static final String SERVER = "http://172.29.128.99:8002/socketHelloWorld/ChatServlet";
    // arguments are added to this to vary the communication with ChatServlet

  private String userName;  // for this client
  private String cookieStr = null;
  
  private JTextArea jtaMesgs;   // GUI components
  private JTextField jtfMsg;
  private JButton jbWho;


  public URLChat(String nm)
  {
     super( "URL Chat Client for "+ nm);

     userName = nm;
     initializeGUI();

     // set the properties used for URL timeouts (in ms)
     Properties props = System.getProperties();
     props.put("sun.net.client.defaultConnectTimeout", "2000");
     props.put("sun.net.client.defaultReadTimeout", "2000");
     System.setProperties(props);

     sayHi();
     // new URLChatWatcher(this, userName, cookieStr).start();    

     addWindowListener( new WindowAdapter() {
       public void windowClosing(WindowEvent e)
       { sayBye(); }
     });

     setSize(300,450);
     setVisible(true);

     new URLChatWatcher(this, userName, cookieStr).start();    
         // start polling the server, getting new chat messages
         // which are written into the jtaMesgs text area
             // change: moved so window visible before contact

  } // end of URLChat();


  private void initializeGUI()
  /* Text area in center, and controls below.
     Controls:
         - textfield for entering messages
         - a "Who" button
  */
  {
    Container c = getContentPane();
    c.setLayout( new BorderLayout() );

    jtaMesgs = new JTextArea(7, 7);
    jtaMesgs.setEditable(false);
    JScrollPane jsp = new JScrollPane( jtaMesgs);
    c.add( jsp, "Center");

    JLabel jlMsg = new JLabel("Message: ");
    jtfMsg = new JTextField(15);
    jtfMsg.addActionListener(this);    // pressing enter triggers sending of name/score

    jbWho = new JButton("Who");
    jbWho.addActionListener(this);

    JPanel p1 = new JPanel( new FlowLayout() );
    p1.add(jlMsg); p1.add(jtfMsg);

    JPanel p2 = new JPanel( new FlowLayout() );
    p2.add(jbWho);

    JPanel p = new JPanel();
    p.setLayout( new BoxLayout(p, BoxLayout.Y_AXIS));
    p.add(p1); p.add(p2);

    c.add(p, "South");

  }  // end of initializeGUI()



  private void sayHi()
  /* Message format: ChatServlet?cmd=hi&name=??         
     The client asks to join the chat group. 
     The server returns a uid cookie as a header field, 
     or rejects the client. A rejection causes the client to exit.
     The textual response can be "ok" or "no".
  */
  {
    try {
      URL url  = new URL(SERVER + "?cmd=hi&name=" +  
							URLEncoder.encode(userName, "UTF-8") );

      URLConnection conn = url.openConnection();
      cookieStr = conn.getHeaderField("Set-Cookie");  // get the cookie

      System.out.println("Received cookie: " + cookieStr);
      if (cookieStr != null) {
        int index = cookieStr.indexOf(";"); 
        if (index != -1) 
          cookieStr = cookieStr.substring(0, index);  // strip away extras
      }

	  BufferedReader br = new BufferedReader(
              new InputStreamReader( conn.getInputStream() ));	
      String response = br.readLine().trim();
      br.close();

      if (response.equals("ok") && (cookieStr != null))
        showMsg("Server Login Successful\n");
      else { 
        System.out.println("Server Rejected Login"); 
        System.exit(0);
      }
    }
    catch(Exception e)
    { System.out.println(e);  
      System.exit(0);
    }
  }  // end of sayHi()



  private void sayBye()
  /* ChatServlet?cmd=bye&name=?? + uid cookie
     The bye message signals the client's departure to the server.
     The response will be "ok" or "no", but the client
     exits whatever the response.
  */
  {
    try {
      URL url  = new URL(SERVER + "?cmd=bye&name=" + 
							URLEncoder.encode(userName, "UTF-8") );
      URLConnection conn = url.openConnection();
      conn.setRequestProperty("Cookie", cookieStr); 

	  BufferedReader br = new BufferedReader(
              new InputStreamReader( conn.getInputStream() ));	
      String response = br.readLine().trim();
      br.close();

      if (response.equals("ok"))
        System.out.println("Server Logout Successful");
      else // assume "no"
        System.out.println("Server Rejected Logout"); 
    }
    catch(Exception e)
    {  System.out.println( e );  }

    System.exit(0); 
  } // end of sayBye()




   public void actionPerformed(ActionEvent e)
   /* Either a message is to be sent or the "Who"
      button has been pressed. */
   { if (e.getSource() == jbWho)
       askWho();
     else if (e.getSource() == jtfMsg)
       sendMessage();
   } // end of actionPerformed()


  private void askWho()
  /*  Message format: ChatServlet?cmd=who
      The message does not require a name argument or a cookie.
      The response is a list of who is currently logged on,
      or "no". The list is displayed in the jtsMesgs text area.
  */
  {
    try {
      URL url  = new URL(SERVER + "?cmd=who" );

	  BufferedReader br = new BufferedReader(
              new InputStreamReader( url.openStream() ));

      String line;
      StringBuffer resp = new StringBuffer();
      while ((line = br.readLine()) != null)   // multiple lines sent back
        resp.append(line+"\n");
      br.close();

      String response = resp.toString();
      if (response.equals("no"))
        showMsg("Server Rejected Who Request\n");
      else // assume there is a list to display
        showMsg(response); 
    }
    catch(Exception e)
    { showMsg("Servlet Error. Who button not processed\n");
      System.out.println(e);  
    }
  }  // end of askWho()


  private void sendMessage()
  /* Check if the user has supplied a message, then
     send it to the servlet. */
  {
    String msg = jtfMsg.getText().trim();
    // System.out.println("'"+msg+"'");

    if (msg.equals(""))
      JOptionPane.showMessageDialog( null, 
           "No message entered", "Send Message Error", 
			JOptionPane.ERROR_MESSAGE);
    else {
      sendURLMessage(msg);
      // showMsg("Sent: " + msg + "\n");
    }
  }  // end of sendMessage()



  private void sendURLMessage(String msg)
  /* Message format: 
        ChatServlet?cmd=msg&name=??&msg=?? + uid cookie

     The message is added to the servlet's list.
     The response is "ok" or "no".
  */
  {
    try {
      URL url  = new URL(SERVER + "?cmd=msg&name=" +  
							URLEncoder.encode(userName, "UTF-8") +
                            "&msg=" + 
							URLEncoder.encode(msg, "UTF-8") );

      URLConnection conn = url.openConnection();
      conn.setRequestProperty("Cookie", cookieStr); 

	  BufferedReader br = new BufferedReader(
              new InputStreamReader( conn.getInputStream() ));	
      String response = br.readLine().trim();
      br.close();

      if (!response.equals("ok"))
        showMsg("Message Send Rejected\n");
      else // display message immediately
        showMsg("(" + userName + ") " + msg + "\n");
    }
    catch(Exception e)
    { showMsg("Servlet Error. Did not send: " + msg + "\n");
      System.out.println(e);  
    }
  }  // end of sendURLMessage()


/*
  synchronized public void showMsg(String msg)
  // Synchronized since this method can be called by this
  //   object and the URLChatWatcher thread.
  { jtaMesgs.append(msg);  }
*/

  public void showMsg(final String msg)
  /* We're updating the messages text area, so the code should
     be carried out by Swing's event dispatching thread, which is 
     achieved by calling invokeLater(). 

     msg must be final to be used inside the inner class for Runnable.

     showMsg() may be called by this object and the URLChatWatcher 
     thread, but the updates are serialised by being placed in the
     queue associated with the event dispatcher. This means that
     there's no need to synchronize this method.

     Thanks to Rachel Struthers (rmstruthers@mn.rr.com)
  */
  { 
    // System.out.println("showMsg(): " + msg);
    Runnable updateMsgsText = new Runnable() {
      public void run() 
      { jtaMesgs.append(msg);  // append message to text area
        jtaMesgs.setCaretPosition( jtaMesgs.getText().length() );
            // move insertion point to the end of the text
      }
    };
    SwingUtilities.invokeLater( updateMsgsText );
  } // end of showMsg()



  // ------------------------------------

  public static void main(String args[]) 
  { if (args.length != 1) {
       System.out.println("usage:  java URLChat <your userName>");
       System.exit(0);
     }
     new URLChat(args[0]);  
  }

} // end of URLChat class

