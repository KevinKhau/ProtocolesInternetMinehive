package util;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;

public class TFServerSocket extends ServerSocket {

	public TFServerSocket(int port) throws IOException {
		super(port);
	}

	@Override
	public TFSocket accept() throws IOException {
		// TODO Auto-generated method stub
		if (isClosed())
            throw new SocketException("Socket is closed");
        if (!isBound())
            throw new SocketException("Socket is not bound yet");
        TFSocket s = new TFSocket();
        implAccept(s);
        s.init(); 
        return s;
	}
	
}
