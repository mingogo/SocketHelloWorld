package com.mteng.chatServlet;
// ChatGroup.java
// Andrew Davison, April 2005, ad@fivedots.coe.psu.ac.th

/* ChatGroup maintains two ArrayLists: chatUsers and messages.

  chatUsers is an ArrayList of Chatter objects; each Chatter
  object stores a client's name, UID, and the number of messages 
  that they have currently read.

  messages is an ArrayList of strings (chat messages).
  When there are no users, the list is cleared.

  A new user is only added if there is no other user
  with the same name.

  Most operations depend on there being a Chatter object with
  a specified name and UID.

  All the public methods are synchronized since there may be many
  doGet() servlet threads wishing to access the ChatGroup object
  at the same time.
*/

import java.util.*;


public class ChatGroup
{
  private ArrayList chatUsers;
  private ArrayList messages;
  private int numUsers;


  public ChatGroup()
  {  chatUsers = new ArrayList();  
     messages = new ArrayList();
     numUsers = 0;
  }


  synchronized public int addUser(String name)
  // adds a user, returns UID if okay, -1 otherwise
  {
    if (numUsers == 0)   // no one logged in
      messages.clear();

    if (isUniqueName(name)) {
      Chatter c = new Chatter(name);
      chatUsers.add(c);  
      messages.add("(" + name + ") has arrived");
      numUsers++;
      return c.getUID();
    }
    return -1;
  }


  private boolean isUniqueName(String name)
  /* Returns true if there is no existing Chatter object with
     the given name. */
  { Chatter c;
    for(int i=0; i < chatUsers.size(); i++) {
      c = (Chatter) chatUsers.get(i);
      if ( c.getUserName().equals(name) )
        return false;
    }
    return true;
  }  // end of findUser()



  synchronized public boolean delUser(String name, int uid)
  // delete the specified user
  { if (uid == -1)
      return false;

    Chatter c;
    for(int i=0; i < chatUsers.size(); i++) {
      c = (Chatter) chatUsers.get(i);
      if (c.matches(name, uid)) {
        chatUsers.remove(i);
        messages.add("(" + name + ") has departed");
        numUsers--;
        return true;
      }
    }
    return false;
  }  // end of delUser()


  private Chatter findUser(String name, int uid)
  // returns Chatter object if it exists, null otherwise
  { if (uid == -1)
      return null;

    Chatter c;
    for(int i=0; i < chatUsers.size(); i++) {
      c = (Chatter) chatUsers.get(i);
      if (c.matches(name, uid))
        return c;
    }
    return null;
  }  // end of findUser()



  synchronized public boolean storeMessage(String name, int uid, String msg)
  /* Add msg to the messages list. It is up to the clients
     to read it by sending "read" messages.
  */
  { Chatter c = findUser(name, uid);
    if (c != null) {
      messages.add("(" + name + ") " + msg);
      return true;
    }
    return false;
  }  // end of storeMessage()


  synchronized public String read(String name, int uid)
  /* Read all the unread messages since the last "read" message.

     A message may be invisible -- it may be addressed to a
     single person by using the message format:
          msg / toName

     Message of this kind are not added to the list returned
     to the client.
  */
  { StringBuffer msgs = new StringBuffer();
    Chatter c = findUser(name, uid);

    if (c != null) {
      int msgsIndex = c.getMsgsIndex();  // where read to last time
      String msg;
      for(int i=msgsIndex; i < messages.size(); i++) {
        msg = (String) messages.get(i);
        if (isVisibleMsg(msg, name))
          msgs.append( msg + "\n" );
      }
      c.setMsgsIndex( messages.size() );  // update client's read index
    }
    return msgs.toString();
  }  // end of read()


  private boolean isVisibleMsg(String msg, String name)
  /* A message is visible if it has no "/ name" part, or 
     "/ name" is the user, or the message is _from_ the user.
  */
  {
    int index = msg.indexOf("/");
    if (index == -1)  // no '/', so message is public
      return true;

    // does have a "/ name" part
    String toName = msg.substring(index+1).trim();
    if (toName.equals(name))  // for this user
      return true;
    else {   // for another user
      if (msg.startsWith("("+name))   // but from this user
        return true;
      else     // from someone else
        return false;
    }
  }  // end of isVisibleMsg()



  synchronized public String who()
  //  Returns a list of who is currently logged on
  { Chatter c;
    StringBuffer whoList = new StringBuffer();
    for(int i=0; i < chatUsers.size(); i++) {
      c = (Chatter) chatUsers.get(i);
      whoList.append("" + (i+1) + ". " + c.getUserName() + "\n");
    }
    return whoList.toString();
  }  // end of who()


}  // end of ChatGroup class
