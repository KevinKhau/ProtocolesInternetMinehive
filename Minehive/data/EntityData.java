package data;

import javax.xml.bind.annotation.XmlTransient;

public class EntityData {

	@XmlTransient
	public String name;

	public EntityData(String name) {
		this.name = name;
	}
	
	
}
