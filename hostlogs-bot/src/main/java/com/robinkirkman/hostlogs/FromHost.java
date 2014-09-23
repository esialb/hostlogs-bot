package com.robinkirkman.hostlogs;

public class FromHost {
	private String to;
	private String host;
	private Integer lines = 100;
	
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
	
}
