
public class ChatMessage implements java.io.Serializable {

	private static final long serialVersionUID = 4834427143649609628L;
	private String who;
	private String type;
	private String text;
	private String data;
	
	public ChatMessage(String who, String type) {
		this.who = who;
		this.type = type;
	}
	
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public String getData() {
		return data;
	}
	public void setData(String data) {
		this.data = data;
	}
	public String getWho() {
		return who;
	}
	public String getType() {
		return type;
	}
}
