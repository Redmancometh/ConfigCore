package com.redmancometh.configcore.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Modifier;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author Redmancometh
 *
 * @param <T>
 */
@Data
public class ConfigManager<T> {
	@Getter
	protected Gson gson;
	protected String fileName;
	protected Class clazz;
	protected T config;
	private FileWatcher watcher;
	@Getter
	@Setter
	private Runnable onReload;

	public ConfigManager(String fileName, Class clazz) {
		this(fileName, clazz, null);
	}

	public ConfigManager(String fileName, Class clazz, Runnable onReload) {
		this(fileName, clazz, onReload, null);

	}

	public ConfigManager(String fileName, Class clazz, Runnable onReload, GsonBuilder gsonBuilder) {
		super();
		this.fileName = fileName;
		this.clazz = clazz;
		this.onReload = onReload;
		if (gsonBuilder == null)
			gsonBuilder = new GsonBuilder();
		gson = gsonBuilder.excludeFieldsWithModifiers(Modifier.PROTECTED)
				.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
				.registerTypeHierarchyAdapter(String.class, new PathAdapter())
				.registerTypeHierarchyAdapter(Material.class, new MaterialAdapter())
				.registerTypeHierarchyAdapter(Effect.class, new EffectAdapter())
				.registerTypeHierarchyAdapter(EntityType.class, new EntityTypeAdapter())
				.registerTypeHierarchyAdapter(PotionEffect.class, new PotionEffectAdapter())
				.registerTypeAdapter(Location.class, new LocationAdapter())
				.registerTypeAdapter(BlockFace.class, new BlockFaceAdapter())
				.registerTypeHierarchyAdapter(Class.class, new ClassAdapter()).setPrettyPrinting().create();
	}

	public void init() {
		initConfig();
		registerMonitor();
	}

	/**
	 * Register the file monitor
	 * 
	 * TODO: This will reload every config any time ANYTHING in the config dir is
	 * changed. So compartmentalize this later.
	 * 
	 */
	public void registerMonitor() {
		watcher = new FileWatcher((file) -> {
			System.out.println("Reloaded: " + file);
			this.initConfig();
			if (this.onReload != null)
				this.onReload.run();
		}, new File("config" + File.separator + this.fileName));
		watcher.start();
	}

	public void writeConfig() {
		try (FileWriter w = new FileWriter("config" + File.separator + this.fileName)) {
			getGson().toJson(config, w);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void saveConfig() {
		try (FileWriter writer = new FileWriter(new File("config" + File.separator + this.fileName))) {
			gson.toJson(getConfig(), writer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void initConfig() {
		File confFile = new File("config" + File.separator + fileName);
		try (FileInputStream inputStream = new FileInputStream(confFile)) {
			try (Reader in = new InputStreamReader(inputStream, "UTF-8")) {
				T conf = (T) getGson().fromJson(in, clazz);
				this.config = conf;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public T targetUnit() {
		return config;
	}

	public void setConfig(T config) {
		this.config = config;
	}

	public static class BlockFaceAdapter extends TypeAdapter<BlockFace> {

		@Override
		public BlockFace read(JsonReader arg0) throws IOException {
			String materialValue = arg0.nextString();
			return BlockFace.valueOf(materialValue.replace(" ", "_").toUpperCase());
		}

		@Override
		public void write(JsonWriter arg0, BlockFace arg1) throws IOException {
			arg0.value(arg1.toString());
		}
	}

	public static class EffectAdapter extends TypeAdapter<Effect> {

		@Override
		public Effect read(JsonReader arg0) throws IOException {
			String materialValue = arg0.nextString();
			return Effect.valueOf(materialValue.replace(" ", "_").toUpperCase());
		}

		@Override
		public void write(JsonWriter arg0, Effect arg1) throws IOException {
			arg0.value(arg1.toString());
		}
	}

	public static class EntityTypeAdapter extends TypeAdapter<EntityType> {

		@Override
		public EntityType read(JsonReader arg0) throws IOException {
			String materialValue = arg0.nextString();
			return EntityType.valueOf(materialValue.replace(" ", "_").toUpperCase());
		}

		@Override
		public void write(JsonWriter arg0, EntityType arg1) throws IOException {
			arg0.value(arg1.toString());
		}
	}

	public static class MaterialAdapter extends TypeAdapter<Material> {

		@Override
		public Material read(JsonReader arg0) throws IOException {
			String materialValue = arg0.nextString();
			return Material.valueOf(materialValue.replace(" ", "_").toUpperCase());
		}

		@Override
		public void write(JsonWriter arg0, Material arg1) throws IOException {
			arg0.value(arg1.toString());
		}
	}

	public static class ClassAdapter extends TypeAdapter<Class> {
		@Override
		public void write(JsonWriter jsonWriter, Class material) throws IOException {

		}

		@Override
		public Class<?> read(JsonReader jsonReader) throws IOException {
			String className = jsonReader.nextString();
			try {
				return Class.forName(className);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			return null;
		}
	}

	public static class PotionEffectAdapter extends TypeAdapter<PotionEffect> {

		@Override
		public PotionEffect read(JsonReader reader) throws IOException {
			reader.beginObject();
			JsonToken token = reader.peek();
			PotionEffectType type = null;
			int duration = 0;
			int amplifier = 0;
			while (reader.hasNext()) {
				if (token.equals(JsonToken.NAME)) {
					String fieldName = reader.nextName();
					if (fieldName.equalsIgnoreCase("effect")) {
						type = PotionEffectType.getByName(reader.nextString().toUpperCase());
					} else if (fieldName.equalsIgnoreCase("duration")) {
						duration = reader.nextInt();
					} else if (fieldName.equalsIgnoreCase("amplifier")) {
						amplifier = reader.nextInt();
					}
				}
			}
			reader.endObject();
			return new PotionEffect(type, duration, amplifier);
		}

		@Override
		public void write(JsonWriter arg0, PotionEffect arg1) throws IOException {

		}

	}

	public static class LocationAdapter extends TypeAdapter<Location> {
		@Override
		public Location read(JsonReader reader) throws IOException {
			reader.beginObject();
			JsonToken token = reader.peek();
			Double x = null;
			Double y = null;
			Double z = null;
			String worldName = null;
			while (reader.hasNext()) {
				if (token.equals(JsonToken.NAME)) {
					String fieldName = reader.nextName();
					if (fieldName.equalsIgnoreCase("x")) {
						System.out.println("X");
						x = reader.nextDouble();
					} else if (fieldName.equalsIgnoreCase("y")) {
						System.out.println("Y");
						y = reader.nextDouble();
					} else if (fieldName.equalsIgnoreCase("z")) {
						System.out.println("Z");
						z = reader.nextDouble();
					} else if (fieldName.equalsIgnoreCase("world")) {
						System.out.println("WORLD");
						worldName = reader.nextString();
					}
				}
			}
			if (x == null)
				throw new IllegalStateException("Invalid config on location (x is null) ");
			else if (y == null)
				throw new IllegalStateException("Invalid config on location (y is null) ");
			else if (z == null)
				throw new IllegalStateException("Invalid config on location (z is null) ");
			else if (worldName == null)
				throw new IllegalStateException("Invalid world on location ");
			if (Bukkit.getWorld(worldName) == null)
				// might not want to use this.
				throw new IllegalStateException(
						"Location contains areference to a world that does not exist called " + worldName);
			reader.endObject();
			return new Location(Bukkit.getWorld(worldName), x.doubleValue(), y.doubleValue(), z.doubleValue());
		}

		@Override
		public void write(JsonWriter arg0, Location arg1) throws IOException {
			arg0.value(arg1.toString());
		}

	}

	public static class PathAdapter extends TypeAdapter<String> {
		@Override
		public String read(JsonReader arg0) throws IOException {
			String string = arg0.nextString();
			if (string.contains("http") || string.contains("jdbc"))
				return string;
			return ChatColor.translateAlternateColorCodes('&',
					string.replace("//", File.separator).replace("\\", File.separator));
		}

		@Override
		public void write(JsonWriter arg0, String arg1) throws IOException {
			arg0.value(arg1);
		}
	}

}
