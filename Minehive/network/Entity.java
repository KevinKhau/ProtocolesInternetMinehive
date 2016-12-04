package network;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import util.Params;

public abstract class Entity {

	public static final Path CREDITS = Paths.get(Params.RES.toString(), "credits");
	
	protected String name;
	protected Logger LOGGER;

	public Entity(String name) {
		this.name = name;

		this.LOGGER = Logger.getLogger(getClass().getSimpleName());
		try {
			Files.createDirectories(Params.LOG);
			DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
			Date date = new Date();
			Path logPath = Paths.get(Params.LOG.toString(),
					getClass().getSimpleName() + "Log" + dateFormat.format(date) + ".xml");
			LOGGER.addHandler(new FileHandler(logPath.toString()));
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Présente le jeu.
	 * @throws IOException 
	 */
	public List<String> credits() throws IOException {
		try (Stream<String> stream = Files.lines(CREDITS)) {
			return stream.collect(Collectors.toList());
		}
	}
	
}
