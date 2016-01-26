package org.openmrs.module.sync.serialization;

import java.util.Properties;


public class PropertiesNormalizer extends Normalizer {

	
	@Override
	public String toString(Object o) {
		if (o != null && o instanceof Properties){
			Properties ps = (Properties) o;
			StringBuilder sb = new StringBuilder("");
			if (ps.size() == 0){
				return "";
			} else {
				for (Object key : ps.keySet()) {
					sb.append("{{");
					sb.append(key);
					sb.append("||");
					sb.append(ps.get(key));
					sb.append("}}");
					
				}
			}
			return sb.toString();
		}
		if (o != null)
			log.error("Sync PropertiesNormalizer was passed an object of class " + o.getClass());
		return null;
	}
	
    @Override
    public Object fromString(Class clazz, String s) {
    	Properties ps = new Properties();
		
		if (!Properties.class.isAssignableFrom(clazz) || s == null) {
			log.error("PropertiesNormalizer incorrectly chosen to deserialize object of type " + clazz.getName());
			return null;
		}
		
		//if it is empty string return empty map
		if (s.length() == 0) {
			return ps;
		}
		
		// strip leading and closing {{ }} since we're splitting it
		if (!s.startsWith("{{") || s.lastIndexOf("}}") < 3) {
			//this isn't right, we are expecting {{k,v}}||{{k,v}}...
			throw new IllegalArgumentException(
			        "Invalid serialization format for Properties object. {{key||value}}{{key||value}} expected.");
		}
		
		String tmp = s;
		tmp = tmp.replaceFirst("\\{\\{", "");
		tmp = tmp.substring(0, tmp.lastIndexOf("}}"));
		
		for (String entry : tmp.split("\\}\\}\\{\\{")) {
			//entry = entry.trim(); // take out whitespace
			if (entry.contains("||")) {
				String keyvalArray[] = entry.split("\\|\\|");
				ps.put(keyvalArray[0], keyvalArray[1]);
			} else {
				throw new IllegalArgumentException(
				        "Invalid serialization format for Properties entry. {{key||value}} expected. encountered:" + entry);
			}
		}
		
		return ps;
    }
}
