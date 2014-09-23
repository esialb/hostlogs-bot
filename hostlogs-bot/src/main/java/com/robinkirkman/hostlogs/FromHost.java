package com.robinkirkman.hostlogs;

public class FromHost implements Comparable<FromHost> {
	private String to;
	private String host;
	private Integer lines = 100;
	
	public FromHost() {}
	
	public FromHost(String host) {
		this.host = host;
	}
	
	public String getTo() {
		return to;
	}
	public void setTo(String to) {
		this.to = to;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public Integer getLines() {
		return lines;
	}
	public void setLines(Integer lines) {
		this.lines = lines;
	}
	
	@Override
	public String toString() {
		return String.valueOf(host);
	}
	@Override
	public int compareTo(FromHost o) {
		if(host == null && o.host == null)
			return 0;
		if(host == null)
			return -1;
		if(o.host == null)
			return 1;
		return String.CASE_INSENSITIVE_ORDER.compare(host, o.host);
	}
}
