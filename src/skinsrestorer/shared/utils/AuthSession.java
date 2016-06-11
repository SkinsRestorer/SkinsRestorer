package skinsrestorer.shared.utils;

public class AuthSession {

	private String name;
	private String id;
	private String authtoken;
	private String clienttoken;

	public AuthSession(String name, String id, String authtoken, String clienttoken) {
		this.name = name.toLowerCase();
		this.id = id;
		this.authtoken = authtoken;
		this.clienttoken = clienttoken;
	}

	public String getName() {
		return name;
	}

	public String getId() {
		return id;
	}

	public String getAuthToken() {
		return authtoken;
	}

	public String getClientToken() {
		return clienttoken;
	}

	public void logout() {
		try {
			MojangAuthAPI.invalidate(authtoken, clienttoken);
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.clienttoken = null;
		this.authtoken = null;
		this.name = null;
	}

}
