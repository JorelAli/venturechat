package mineverse.Aust1n46.chat.listeners;

import java.util.Iterator;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.bukkit.entity.Player;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import me.clip.placeholderapi.PlaceholderAPI;
import mineverse.Aust1n46.chat.ChatMessage;
import mineverse.Aust1n46.chat.MineverseChat;
import mineverse.Aust1n46.chat.api.MineverseChatAPI;
import mineverse.Aust1n46.chat.api.MineverseChatPlayer;
import mineverse.Aust1n46.chat.utilities.Format;
import mineverse.Aust1n46.chat.versions.VersionHandler;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;

//This class listens for chat packets and intercepts them before they are sent to the Player.
//The packets are modified to include advanced json formating and the message remover button if the
//player has permission to remove messages.
public class PacketListener extends PacketAdapter {
	public PacketListener(MineverseChat plugin) {
		super(plugin, ListenerPriority.MONITOR, new PacketType[] { PacketType.Play.Server.CHAT });
		this.plugin = plugin;
	}
	
	//Thing to say in chat to get stuff to work
	final String ITEM_CHAT_CONSTANT = "[item2]";

	@Override
	public void onPacketSending(PacketEvent event) {
		if(event.isCancelled() || event.getPacketType() != PacketType.Play.Server.CHAT) {
			return;
		}

		StructureModifier<WrappedChatComponent> chatP = event.getPacket().getChatComponents();
		WrappedChatComponent c = chatP.read(0);
		if (c == null) {
			StructureModifier<BaseComponent[]> modifier = event.getPacket().getSpecificModifier(BaseComponent[].class);
			BaseComponent[] components = modifier.readSafely(0);
			if (components == null) {
				return;
			}
			String msg = ComponentSerializer.toString(components);
			if (msg == null) {
				return;
			}
			if (!PlaceholderAPI.getBracketPlaceholderPattern().matcher(msg).find()) {
				return;
			}
			msg = PlaceholderAPI.setBracketPlaceholders(event.getPlayer(), msg);
			modifier.write(0, ComponentSerializer.parse(msg));
			return;
		}
		String msg = c.getJson();
		if (msg == null) {
			return;
		}
		if (!PlaceholderAPI.getBracketPlaceholderPattern().matcher(msg).find()) {
			return;
		}
		
		//Reconstructs the JSON based on the stuff the player's holding
		msg = reconstructJSON(event.getPlayer(), msg);
		
		chatP.write(0, WrappedChatComponent.fromJson(msg));

		try {
			if(VersionHandler.is1_7_10() || VersionHandler.is1_7_9() || VersionHandler.is1_7_2()) {
				if((MineverseChat.posField != null) && !(((boolean) MineverseChat.posField.get(event.getPacket().getHandle())))) {
					return;
				}
			}
			else if(VersionHandler.is1_8()) {
				if((MineverseChat.posField != null) && (((Byte) MineverseChat.posField.get(event.getPacket().getHandle())).intValue() > 1)) {
					return;
				}
			}
			else if(VersionHandler.is1_9() || VersionHandler.is1_10() || VersionHandler.is1_11()){
				if((MineverseChat.posField != null) && (((Byte) MineverseChat.posField.get(event.getPacket().getHandle())).intValue() > 1)) {
					return;
				}
			}
			else {
				if((MineverseChat.posField != null) && ((Object) MineverseChat.posField.get(event.getPacket().getHandle())) == MineverseChat.chatMessageType.getEnumConstants()[2]) {
					return;
				}
			}
		}
		catch(IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		WrappedChatComponent chat = (WrappedChatComponent) event.getPacket().getChatComponents().read(0);
		WrappedChatComponent originalChat = (WrappedChatComponent) event.getPacket().getChatComponents().read(0);
		String message = null;
		int hash = -1;
		try {
			//System.out.println(chat.getJson());
			//message = TextComponent.toPlainText(new TextComponent(chat.getJson()));
			message = (String) MineverseChat.messageMethod.invoke(chat.getHandle(), new Object[0]);
			//System.out.println(MineverseChat.lastChatMessage.getMessage());
			hash = message != null ? message.hashCode() : -1;
		}
		catch(Exception ex) {
			message = TextComponent.toPlainText(new TextComponent(chat.getJson()));
			System.out.println(message);
			//ex.printStackTrace();
		}
		ChatMessage lastChatMessage = MineverseChat.lastChatMessage;
		MineverseChatPlayer mcp = MineverseChatAPI.getMineverseChatPlayer(event.getPlayer());
		if(lastChatMessage != null && lastChatMessage.getHash() == hash) {
			String json = MineverseChat.lastJson;
			/*if(mcp.getPlayer().hasPermission("venturechat.message.remove")) {
				json = json.substring(0, json.length() - 1);
				json += ",{\"text\":\" " + Format.FormatStringAll(plugin.getConfig().getString("messageremovericon")) + "\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/removemessage " + lastChatMessage.getHash() + " true" +"\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":{\"text\":\"" + Format.FormatStringAll(plugin.getConfig().getString("messageremovertext")) + "\"}}}]";
			}
			if(!mcp.getButtons()) {
				for(JsonButton j : MineverseChat.jbInfo.getJsonButtons()) {
					if(j.hasPermission() && mcp.getPlayer().hasPermission(j.getPermission())) {
						json = json.substring(0, json.length() - 1);
						json += ",{\"text\":\" " + Format.FormatStringAll(j.getIcon()) + "\",\"clickEvent\":{\"action\":\"" + j.getAction() + "\",\"value\":\"/" + j.getCommand().replace("{channel}", lastChatMessage.getChannel()).replace("{player}", lastChatMessage.getSender()) + "\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":{\"text\":\"" + Format.FormatStringAll(j.getText()) + "\"}}}]";
					}
				}
			}*/
			if(mcp.getPlayer().hasPermission("venturechat.gui")) {
				json = json.substring(0, json.length() - 1);
				json += "," + Format.convertToJsonColors(Format.FormatStringAll(plugin.getConfig().getString("guiicon")), ",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/vchatgui " + lastChatMessage.getSender() + " " + lastChatMessage.getChannel() + " " + lastChatMessage.getHash() +"\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":{\"text\":\"\",\"extra\":[" + Format.convertToJsonColors(Format.FormatStringAll(plugin.getConfig().getString("guitext"))) + "]}}") + "]}]";
			}
			//System.out.println(json);
			chat.setJson(json);
			event.getPacket().getChatComponents().write(0, chat);
		}
		if((message != null) && (chat.getHandle() != null) && mcp != null) {
			mcp.addMessage(new ChatMessage(originalChat, chat, lastChatMessage != null ? lastChatMessage.getSender() : null, message, hash));
		}
	}

	@SuppressWarnings("unused")
	private String getMessage(String json) {
		JSONArray components = (JSONArray) ((JSONObject) JSONValue.parse(json)).get("extra");
		Iterator<?> iterator = components.iterator();
		StringBuilder builder = new StringBuilder();
		while(iterator.hasNext()) {
			builder.append(((JSONObject) iterator.next()).get("text").toString());
		}
		return builder.toString();
	}
	
	//Builds the JSON to send to the player
	private String reconstructJSON(Player player, String json) {
		
		System.out.println("Reconstructing json for packet");
		System.out.println("Current JSON:");
		System.out.println(json);
		JsonObject obj = new Gson().fromJson(json, JsonObject.class);
		JsonArray array = obj.get("extra").getAsJsonArray();
		
		//List<Integer> indexesToReconstruct = new ArrayList<>();
		TreeMap<Integer, JsonObject> indexesToReconstruct = new TreeMap<>();
		
		for(int i = 0; i < array.size(); i++) {
			JsonElement element = array.get(i);
			if(element.isJsonObject()) {
				JsonObject object = element.getAsJsonObject();
				if(object.get("text").getAsString().contains(ITEM_CHAT_CONSTANT)) {
					//Need to reconstruct the two things before and after [item2]
					indexesToReconstruct.put(i, object);					
				}
			}
		}
		
		//Requires placeholder API... for some reason? (I never moved the stuff over to here... I should probably do that)
		String insertJson = PlaceholderAPI.setBracketPlaceholders(player, "{currentitem_item}");
		JsonArray insertArray = new Gson().fromJson(insertJson, JsonArray.class);
		JsonElement reconstruction = insertArray.get(1);
		
		for(int i : indexesToReconstruct.keySet()) {
			System.out.println("Reconstructing index " + i);
			JsonArray newArray = new JsonArray();
		    for (int j = 0; j < i; j++) { newArray.add(array.get(j)); }
		    
		    String[] splitText = indexesToReconstruct.get(i).get("text").getAsString().split(Pattern.quote(ITEM_CHAT_CONSTANT));
		    if(splitText.length == 0) {
		    	newArray.add(reconstruction);
		    } else if(splitText.length == 1) {
		    	JsonObject before = indexesToReconstruct.get(i);
		    	before.remove("text");
		    	before.addProperty("text", splitText[0]);
		    	newArray.add(before);
		    	
			    newArray.add(reconstruction);
		    } else if(splitText.length == 2) {
		    	JsonObject before = indexesToReconstruct.get(i);
		    	before.remove("text");
		    	before.addProperty("text", splitText[0]);
		    	newArray.add(before);
		    	
			    newArray.add(reconstruction);
			    
			    JsonObject after = indexesToReconstruct.get(i);
			    after.remove("text");
			    after.addProperty("text", splitText[1]);
			    newArray.add(after);
		    } else {
		    	System.out.println("Length is larger than 2!");
		    	System.out.println(indexesToReconstruct.get(i).get("text"));
		    	//gulp.
		    }

		    for (int j = i; j < array.size(); j++) { newArray.add(array.get(j)); }
		    
		    array = newArray;
		    
		    break;
		}
		
		
		
		obj.remove("extra");
		obj.add("extra", array);
		
		System.out.println("Reconstructed JSON:");
		
		System.out.println(obj.toString());
		
		if(indexesToReconstruct.size() > 1) {
			return reconstructJSON(player, obj.toString());
		}
		return obj.toString();
		
	}
	
	
}