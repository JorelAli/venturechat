package mineverse.Aust1n46.chat.utilities;

import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_13_R2.util.CraftChatMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import net.minecraft.server.v1_13_R2.NBTBase;
import net.minecraft.server.v1_13_R2.NBTNumber;
import net.minecraft.server.v1_13_R2.NBTTagByte;
import net.minecraft.server.v1_13_R2.NBTTagByteArray;
import net.minecraft.server.v1_13_R2.NBTTagCompound;
import net.minecraft.server.v1_13_R2.NBTTagDouble;
import net.minecraft.server.v1_13_R2.NBTTagEnd;
import net.minecraft.server.v1_13_R2.NBTTagFloat;
import net.minecraft.server.v1_13_R2.NBTTagInt;
import net.minecraft.server.v1_13_R2.NBTTagIntArray;
import net.minecraft.server.v1_13_R2.NBTTagList;
import net.minecraft.server.v1_13_R2.NBTTagLong;
import net.minecraft.server.v1_13_R2.NBTTagLongArray;
import net.minecraft.server.v1_13_R2.NBTTagShort;
import net.minecraft.server.v1_13_R2.NBTTagString;

public class ItemToJson {

	public static String playerItemToJSONString(Player p) {
		if(p == null) {
			return null;
		}
		
		ItemStack is = p.getInventory().getItemInMainHand();
		net.minecraft.server.v1_13_R2.ItemStack result = CraftItemStack.asNMSCopy(is);
		
		String itemName = CraftChatMessage.toJSON(result.getName());
		//Deconstruct itemName
		JsonObject itemNameObj = new Gson().fromJson(itemName, JsonObject.class);
		JsonElement itemNameText = itemNameObj.get("text") == null ? itemNameObj.get("translate") : itemNameObj.get("text"); 
		itemName = itemNameText.getAsString();
		
		JsonObject json = nbtToJson(result.save(new NBTTagCompound()));
				
		JsonObject hoverEvent = new JsonObject();
		hoverEvent.addProperty("action", "show_item");
		hoverEvent.addProperty("value", json.toString());
		
		JsonObject textData = new JsonObject();
		textData.addProperty("text", itemName); //TODO: Sort this out - this seems to look dodgy - [Note @ 28th/Jan - this seems to work fine]
		textData.add("hoverEvent", hoverEvent);
		
		JsonArray text = new JsonArray();
		text.add("");
		text.add(textData);

		//Test with ProtocolLib
		//System.out.println(WrappedChatComponent.fromJson(text.toString()).toString());
		
		return text.toString();
	}
	
	//Converts NBTTagCompound to JsonObject
	private static JsonObject nbtToJson(NBTTagCompound nbt) {
		JsonObject object = new JsonObject();
		for(String key : nbt.getKeys()) {
			NBTBase base = nbt.get(key);
			object.add(key, nbtBaseToJson(base));
		}
		return object;
	}
	
	//Converts NBTBase to JsonObject
	private static JsonElement nbtBaseToJson(NBTBase base) {
		if(base instanceof NBTTagByte) {
			return new JsonPrimitive(((NBTTagByte) base).g());
		} else if(base instanceof NBTTagByteArray) {
			JsonArray byteArray = new JsonArray();
			
			NBTTagByteArray list = (NBTTagByteArray) base;
			for(NBTTagByte i : list) {
				byteArray.add(i.g());
			}
			return byteArray;
		} else if(base instanceof NBTTagCompound) {
			return nbtToJson((NBTTagCompound) base);
		} else if(base instanceof NBTTagDouble) {
			return new JsonPrimitive(((NBTNumber) base).asDouble());
		} else if(base instanceof NBTTagEnd) {
			//Erm?
		} else if(base instanceof NBTTagFloat) {
			return new JsonPrimitive(((NBTTagFloat) base).i());
		} else if(base instanceof NBTTagInt) {
			return new JsonPrimitive(((NBTTagInt) base).e());
		} else if(base instanceof NBTTagIntArray) {
			JsonArray intArray = new JsonArray();
			NBTTagIntArray list = (NBTTagIntArray) base;
			for(NBTTagInt i : list) {
				intArray.add(i.e());
			}
			return intArray;
		} else if(base instanceof NBTTagList) {
			JsonArray jsonArray = new JsonArray();
			NBTTagList list = (NBTTagList) base;
			for(NBTBase listElement : list) {
				jsonArray.add(nbtBaseToJson(listElement));
			}
			return jsonArray;
		}  else if(base instanceof NBTTagLong) {
			return new JsonPrimitive(((NBTTagLong) base).d());
		} else if(base instanceof NBTTagLongArray) {
			JsonArray longArray = new JsonArray();
			NBTTagLongArray list = (NBTTagLongArray) base;
			for(NBTTagLong longElement : list) {
				long l = longElement.d();
				longArray.add(l);
			}
			return longArray;
		} else if(base instanceof NBTTagShort) {
			return new JsonPrimitive(((NBTTagShort) base).f());
		} else if(base instanceof NBTTagString) {
			return new JsonPrimitive(base.b_());
		}
		return new JsonObject();
	}
	
}
