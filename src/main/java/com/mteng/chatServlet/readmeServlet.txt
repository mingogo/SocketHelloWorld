
Chapter 30. Network Chat  / ChatServlet

From:
  Killer Game Programming in Java
  Andrew Davison
  O'Reilly, May 2005
  ISBN: 0-596-00730-2
  http://www.oreilly.com/catalog/killergame/
  Web Site for the book: http://fivedots.coe.psu.ac.th/~ad/jg

Contact Address:
  Dr. Andrew Davison
  Dept. of Computer Engineering
  Prince of Songkla University
  Hat Yai, Songkhla 90112, Thailand
  E-mail: ad@fivedots.coe.psu.ac.th

If you use this code, please mention my name, and include a link
to the book's Web site.

Thanks,
  Andrew

---------

This directory contains a Chat application where the clients use 
a servlet as a server.

The client-side classes:
* URLChat
* URLChatWatcher

The servlet-side classes:
* ChatServlet
* ChatGroup
* Chatter

--------------------------
Compilation:

$ javac *.java

The compilation of the servlet code will typically require
extra classpath information, pointing to the JARs for the 
servlet-related packages used by the servlet/J2EE container.

The compilation of the servlet code generates 4 unchecked
warnings in J2SE 5, due to its use of un-generified collections.

----------------------------
Execution:

1. Start the Web server with the servlet and its support classes.

2. Start the clients, e.g.
      $ java URLChat andy
      $ java URLChat paul



The examples are set up to run on the same machine
(i.e. the server's address is localhost).
 
---------
Last updated: 20th April 2005

