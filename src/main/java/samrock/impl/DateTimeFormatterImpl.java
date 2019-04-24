package samrock.impl;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateTimeFormatterImpl implements samrock.api.DateTimeFormatter {
	
	private final ZoneId z = ZoneId.systemDefault();
	private final DateTimeFormatter dataTimeFormatter = DateTimeFormatter.ofPattern("dd,MMM hh:mma");
	private final DateTimeFormatter lastYeardateTimeFormatter = DateTimeFormatter.ofPattern("dd,MMM yyy HH:mm");
	private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mma");
	private final LocalDate today = LocalDate.now();
	private final LocalDate yesterday = today.minusDays(1);
	private final int thisYear = today.getYear();

	public String format(long time) {
		if(time < 10000)
			return "Yet To Be";

		LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(time), z);
		LocalDate d = dt.toLocalDate();

		if(d.equals(today))
			return "Today ".concat(dt.format(timeFormatter));
		else if(d.equals(yesterday))
			return "Yesterday ".concat(dt.format(timeFormatter));
		else if(d.getYear() == thisYear)
			return dt.format(dataTimeFormatter);
		else
			return dt.format(lastYeardateTimeFormatter);
	}

}
