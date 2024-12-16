package dev.geco.gmusic.manager;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;

import dev.geco.gmusic.GMusicMain;
import dev.geco.gmusic.objects.*;
import org.bukkit.persistence.PersistentDataType;

public class SongManager {

    private final GMusicMain GPM;

    public SongManager(GMusicMain GPluginMain) { GPM = GPluginMain; }

    private List<Song> songs = new ArrayList<>();

    private final HashMap<UUID, MusicGUI> musicGUIs = new HashMap<>();

    public HashMap<UUID, MusicGUI> getMusicGUIs() { return musicGUIs; }

    public List<Song> getSongs() { return new ArrayList<>(songs); }

    public Song getSongById(String Song) { return songs.parallelStream().filter(song -> song.getId().equalsIgnoreCase(Song)).findFirst().orElse(null); }

    public void convertAllSongs() {

        File songsDir = new File(GPM.getDataFolder(), "songs");
        if(!songsDir.exists()) songsDir.mkdir();

        File nbsDir = new File(GPM.getDataFolder(), "nbs");
        if(!nbsDir.exists()) nbsDir.mkdir();

        File midiDir = new File(GPM.getDataFolder(), "midi");
        if(!midiDir.exists()) midiDir.mkdir();

        File dataDir = new File(GPM.getDataFolder(), "data");
        if(!dataDir.exists()) dataDir.mkdir();

        Arrays.asList(Objects.requireNonNull(nbsDir.listFiles())).parallelStream().forEach(file -> {

            if(!new File(songsDir.getAbsolutePath() + "/" + file.getName().replaceFirst("[.][^.]+$", "") + ".gnbs").exists()) GPM.getNBSManager().convertFile(file);
        });

        Arrays.asList(Objects.requireNonNull(midiDir.listFiles())).parallelStream().forEach(file -> {

            if(!new File(songsDir.getAbsolutePath() + "/" + file.getName().replaceFirst("[.][^.]+$", "") + ".gnbs").exists()) GPM.getMidiManager().convertFile(file);
        });
    }

    public void loadSongs() {

        songs.clear();

        convertAllSongs();

        File songsDir = new File(GPM.getDataFolder(), "songs");

        songs = Arrays.asList(Objects.requireNonNull(songsDir.listFiles())).parallelStream().map(file -> {

            int pos = file.getName().lastIndexOf(".");
            if(pos <= 0 || !file.getName().substring(pos + 1).equalsIgnoreCase("gnbs")) return null;

            Song song = new Song(file);

            if (song.getTitle() == null){
                song.setTitle(file.getName().replaceFirst("[.][^.]+$", ""));
            };

            if(song.getNoteAmount() == 0) return null;

            ArrayList<Component> description = new ArrayList<>();
            for(String descriptionRow : song.getDescription()) description.add(Component.text(GPM.getMManager().getMessage(descriptionRow)));

            ItemStack itemStack = new ItemStack(song.getMaterial());
            ItemMeta itemMeta = itemStack.getItemMeta();
            itemMeta.displayName(Component.text(GPM.getMManager().getMessage("Items.disc-title", "%Title%", song.getTitle(), "%Author%", song.getAuthor().isEmpty() ? GPM.getMManager().getMessage("MusicGUI.disc-empty-author") : song.getAuthor(), "%OAuthor%", song.getOriginalAuthor().isEmpty() ? GPM.getMManager().getMessage("MusicGUI.disc-empty-oauthor") : song.getOriginalAuthor())));
            NamespacedKey localizedNameKey = new NamespacedKey(GPM, "LocalizedName");
            itemMeta.getPersistentDataContainer().set(localizedNameKey, PersistentDataType.STRING, GPM.NAME + "_D_" + song.getId());
            itemMeta.lore(description);
            itemMeta.addItemFlags(ItemFlag.values());
            itemStack.setItemMeta(itemMeta);

            // this.GPM.getLogger().info("Loaded song: " + song.getTitle());

            return song;
        }).filter(Objects::nonNull).collect(Collectors.toList());

        songs.sort(Comparator.comparing(Song::getTitle, String.CASE_INSENSITIVE_ORDER));
    }

    public void putMusicGUI(UUID UUID, MusicGUI MusicGUI) { getMusicGUIs().put(UUID, MusicGUI); }

    public MusicGUI getMusicGUI(UUID UUID, MusicGUI.MenuType MenuType) {

        MusicGUI musicGUI = getMusicGUIs().get(UUID);

        return musicGUI != null ? musicGUI : new MusicGUI(UUID, MenuType);
    }

}