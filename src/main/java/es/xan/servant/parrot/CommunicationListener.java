package es.xan.servant.parrot;

public interface CommunicationListener {
	void onMessage(String sender, String message);
}
