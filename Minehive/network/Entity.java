package network;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import util.Params;

public abstract class Entity {

	protected String name;
	protected Logger LOGGER;
	
	public Entity(String name) {
		this.name = name;
		
		this.LOGGER = Logger.getLogger(getClass().getSimpleName());
		try {
			Files.createDirectories(Paths.get(Params.DIR_BIN, Params.DIR_LOG));
			DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
			Date date = new Date();
			Path logPath = Paths.get(Params.DIR_BIN, Params.DIR_LOG, getClass().getSimpleName() + "Log" + dateFormat.format(date) + ".xml");
			LOGGER.addHandler(new FileHandler(logPath.toString()));
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
		}
	}
	
}
