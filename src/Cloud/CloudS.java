package Cloud;

/**
 * @author Emanuel Andersson
 */

import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.text.SimpleDateFormat;

/**
 * Main server class
 * @author Emanuel Andersson
 */

class CloudS
{

    ServerSocket        welcomeSocket;
    static String       password;

    /**
     * Main class, handles start input
     * @param argv
     * @throws Exception
     */

    public static void main(String argv[]) throws Exception
    {

        if(argv.length != 2)
        {
            System.out.println("Please specify port and password! Form <port> <password>");
            System.exit(1);
        }
        else
        {
            try
            {
                Integer.parseInt(argv[0]);
                password = md5Enc(argv[1]);
            }
            catch(Exception ex)
            {
                System.out.println("Invalid port or password!");
                System.exit(1);
            }
        }

        /* Start server */
        CloudS server = new CloudS(Integer.parseInt(argv[0]));

   }

   CloudS(int port) throws Exception
   {

       /* Set custom port if applicable */
       try
       {
           System.out.print("\n");
           System.out.println("Server running - " + inConnection.now());
           welcomeSocket = new ServerSocket(port);
       }
       catch(Exception ex)
       {
           System.out.println("Invalid port: " + ex.getMessage());
           System.exit(1);
       }

       /* Accept new connections */
       while(welcomeSocket.isClosed() == false)
       {
           Socket connection = welcomeSocket.accept();

           /* Start streams */
           DataInputStream read = new DataInputStream(connection.getInputStream());
           DataOutputStream write = new DataOutputStream(connection.getOutputStream());

           /* Auth */
           if(read.readUTF().equals(password))
           {
               write.writeUTF("ACCEPTED");

               /* Create new thread for connection */
               inConnection newCon = new inConnection(connection);
               newCon.start();
           }
           else
           {
               write.writeUTF("DENIED");
           }

           write.flush();
       }

       /* Notify that server has stopped and exit */
       System.out.println("Server stopped - " + inConnection.now() + "\n");
       System.exit(1);

   }
   
   /**
    * MD5 encrypt text (not written by Emanuel Andersson)
    * @param text Text to be encrypted
    * @return Encrypted text
    * @throws NoSuchAlgorithmException
    */

   public static String md5Enc(String text) throws NoSuchAlgorithmException
   {

       MessageDigest mdEnc = MessageDigest.getInstance("MD5");
       mdEnc.update(text.getBytes(), 0, text.length());
       return new BigInteger(1, mdEnc.digest()).toString(16);
   }
   
}

/**
 * New connection (to server) thread
 * @author Emanuel Andersson
 */

class inConnection extends Thread
{
    Socket              connection;
    DataInputStream     read;
    DataOutputStream    write;

    inConnection(Socket con) throws Exception
    {
        connection = con;
        read = new DataInputStream(connection.getInputStream());
        write = new DataOutputStream(connection.getOutputStream());
    }

    @Override
    public void run()
    {
        try
        {
            System.out.println("New connection (" + connection.getInetAddress().getHostAddress() + ")");
            startClient();
        }
        catch(Exception e)
        {
            System.out.println("Connection terminated (" + connection.getInetAddress().getHostAddress() + ")");
        }
        finally
        {
            try
            {
                endClient();
            }
            catch(Exception ex)
            {
                System.out.println("Unable to close streams/socket: " + ex.getMessage());
            }
        }
    }

    /**
     * Main connection loop and input handler
     * @throws Exception
     */

    private void startClient() throws Exception
    {
        while(connection.isConnected())
        {

            /* In from client */
            String data = read.readUTF();

            /* Check if client is terminating server */
            if(data.toLowerCase().equals("kill clouds"))
            {
                /* Notify that server has stopped and exit */
                System.out.println("Server stopped - " + now() + "\n");
                System.exit(1);
            }

            /* Execute command on server */
            String execErrors = execCommand(data);

            if(execErrors == null)
            {
                write.writeUTF("executed: \"" + data + "\"");
                System.out.println("executed: \"" + data + "\" by " + connection.getInetAddress().getHostAddress() + " on " + now());
            }
            else
            {
                write.writeUTF("error: \"" + execErrors + "\"");
                System.out.println("error: \"" + data + "\" by " + connection.getInetAddress().getHostAddress() + " on " + now());
            }

            write.flush();

        }
        
    }

    /**
     * Close streams and socket for client
     * @throws Exception
     */

    private void endClient() throws Exception
    {
        connection.close();
        read.close();
        write.close();
    }

    /**
     * Execute shell commands
     * @param command The os command
     * @return
     */

    private String execCommand(String command)
    {
        try
        {
            Runtime.getRuntime().exec(command);
            return null;
        }
        catch(Exception e)
        {
            return e.getMessage();
        }
    }

    /**
     * Get current time
     * @return Current time
     */

    static public String now()
    {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(cal.getTime());
    }
}
