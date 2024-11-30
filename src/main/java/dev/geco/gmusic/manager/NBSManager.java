package dev.geco.gmusic.manager;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import org.bukkit.configuration.file.*;

import dev.geco.gmusic.GMusicMain;

public class NBSManager {

	private final GMusicMain GPM;

	public NBSManager(GMusicMain GPluginMain) { GPM = GPluginMain; }

	public void convertFile(File NBSFile) {

		try {

			DataInputStream dataInput = new DataInputStream(Files.newInputStream(NBSFile.toPath()));

			short type = readShort(dataInput);
			int version = 0;

			if(type == 0) {
				version = dataInput.readByte();
				dataInput.readByte();
				if(version >= 3) readShort(dataInput);
			}

			short header = readShort(dataInput);
			String title = readString(dataInput);
			if(title.isEmpty()) title = NBSFile.getName().replaceFirst("[.][^.]+$", "");
			String author = readString(dataInput);
			String originalAuthor = readString(dataInput);
			String description = readString(dataInput);
			float sequence = readShort(dataInput) / 100f;
			dataInput.readBoolean();
			dataInput.readByte();
			dataInput.readByte();
			readInt(dataInput);
			readInt(dataInput);
			readInt(dataInput);
			readInt(dataInput);
			readInt(dataInput);
			readString(dataInput);

			if(version >= 4) {
				dataInput.readByte();
				dataInput.readByte();
				readShort(dataInput);
			}

			List<String> gnbsContent = new ArrayList<>();
			List<Byte> gnbsInstruments = new ArrayList<>();

			while(true) {

				short jt = readShort(dataInput);
				if(jt == 0) break;

				StringBuilder content = new StringBuilder(((long) ((gnbsContent.isEmpty() ? jt - 1 : jt) * 1000 / sequence)) + "!");

				while(true) {

					short jl = readShort(dataInput);
					if(jl == 0) break;
					byte i = dataInput.readByte();
					byte k = dataInput.readByte();
					int p = 100;

					if(version >= 4) {
						dataInput.readByte();
						p = 200 - dataInput.readUnsignedByte();
						readShort(dataInput);
					}

					String contentPart = i + "::#" + (k - 33) + (p == 100 ? "" : ":" + p);

					content.append(content.toString().endsWith("!") ? contentPart : "_" + contentPart);

					if(!gnbsInstruments.contains(i)) gnbsInstruments.add(i);
				}

				// minify gnbsContent
				if(!gnbsContent.isEmpty()) {

					String[] tick = gnbsContent.get(gnbsContent.size() - 1).split(";");

					if(content.toString().equals(tick[0])) {

						gnbsContent.remove(gnbsContent.size() - 1);

						gnbsContent.add(content + ";" + ((tick.length == 1 || tick[1].isEmpty() ? 0 : Long.parseLong(tick[1])) + 1));

					} else gnbsContent.add(content.toString());
				} else gnbsContent.add(content.toString());
			}

			for(int headerCount = 0; headerCount < header; headerCount++) {
				readString(dataInput);
				if(version >= 4) dataInput.readByte();
				dataInput.readByte();
				if(version >= 2) dataInput.readByte();
			}

			byte midiInstrumentsLength = dataInput.readByte();

			List<String> midiInstruments = new ArrayList<>();

			for(int instrumentCount = 0; instrumentCount < midiInstrumentsLength; instrumentCount++) {
				readString(dataInput);
				midiInstruments.add(readString(dataInput).replace(".ogg", ""));
				dataInput.readByte();
				dataInput.readByte();
			}

			String gnbsFilename = NBSFile.getName();
			int pos = gnbsFilename.lastIndexOf(".");
			if(pos != -1) gnbsFilename = gnbsFilename.substring(0, pos);

			File gnbsFile = new File(GPM.getDataFolder(), "songs/" + gnbsFilename + ".gnbs");

			YamlConfiguration gnbsStruct = YamlConfiguration.loadConfiguration(gnbsFile);

			gnbsStruct.set("Song.Id", title.replace(" ", ""));
			gnbsStruct.set("Song.Title", title);
			gnbsStruct.set("Song.OAuthor", originalAuthor);
			gnbsStruct.set("Song.Author", author);
			gnbsStruct.set("Song.Description", description.replace(" ", "").isEmpty() ? new ArrayList<>() : Arrays.asList(description.split("\n")));
			gnbsStruct.set("Song.Category", "RECORDS");

			for(byte instrument = 0; instrument < 16; instrument++) if(gnbsInstruments.contains(instrument)) gnbsStruct.set("Song.Content.Instruments." + instrument, instrument);

			for(int instrument = 16; instrument < 16 + midiInstruments.size(); instrument++) gnbsStruct.set("Song.Content.Instruments." + instrument, midiInstruments.get(instrument - 16));

			gnbsStruct.set("Song.Content.Main", gnbsContent);

			gnbsStruct.save(gnbsFile);
		} catch (Throwable e) { e.printStackTrace(); }
	}

	private short readShort(DataInputStream DataInput) throws IOException {
		int i1 = DataInput.readUnsignedByte();
		int i2 = DataInput.readUnsignedByte();
		return (short) (i1 + (i2 << 8));
	}

	private int readInt(DataInputStream DataInput) throws IOException {
		int i1 = DataInput.readUnsignedByte();
		int i2 = DataInput.readUnsignedByte();
		int i3 = DataInput.readUnsignedByte();
		int i4 = DataInput.readUnsignedByte();
		return (i1 + (i2 << 8) + (i3 << 16) + (i4 << 24));
	}

	private String readString(DataInputStream DataInput) throws IOException {
		int length = readInt(DataInput);
		StringBuilder builder = new StringBuilder(length);
		for(; length > 0; --length) {
			char c = (char) DataInput.readByte();
			builder.append(c == (char) 0x0D ? ' ' : c);
		}
		return builder.toString();
	}

}