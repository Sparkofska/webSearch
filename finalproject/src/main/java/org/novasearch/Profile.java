package org.novasearch;

public class Profile {
	private String topid;
	private String title;
	private String description;
	private String narrative;

	public Profile() {

	}

	public Profile(String topid, String title, String description, String narrative) {
		this.topid = topid;
		this.title = title;
		this.description = description;
		this.narrative = narrative;
	}

	public String getTopid() {
		return topid;
	}

	public void setTopid(String topid) {
		this.topid = topid;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getNarrative() {
		return narrative;
	}

	public void setNarrative(String narrative) {
		this.narrative = narrative;
	}

	@Override
	public String toString() {
		int maxLength = 30;
		String descShort = shorten(description, maxLength);
		String narrShort = shorten(narrative, maxLength);
		return "Profile [topid=" + topid + ", title=" + title + ", description=" + descShort + ", narrative="
				+ narrShort + "]";
	}

	private String shorten(String narrative2, int maxLength) {
		if (narrative2.length() <= maxLength)
			return narrative2;
		return narrative2.substring(0, maxLength) + "...";
	}
}
