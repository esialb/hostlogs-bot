package com.robinkirkman.hostlogs;

import java.util.List;

public interface LineMapper {
	public void insert(Line line);
	public List<Line> last100(FromHost from);
	
	public void create();
}
