package it.demo.fabrick.dto;

public class ConfigurazioneMessageCodec /* implements MessageCodec<ConfigurazioneDto, ConfigurazioneDto> */ {

//	@Override
//	public void encodeToWire(Buffer buffer, ConfigurazioneDto conf) {
//		// Easiest ways is using JSON object
//		JsonArray jsonToEncode = conf.toJsonArray();
//
//		// Encode object to string
//		String jsonToStr = jsonToEncode.encode();
//
//		// Length of JSON: is NOT characters count
//		int length = jsonToStr.getBytes().length;
//
//		// Write data into given buffer
//		buffer.appendInt(length);
//		buffer.appendString(jsonToStr);
//	}
//
//	@Override
//	public ConfigurazioneDto decodeFromWire(int position, Buffer buffer) {
//		// My custom message starting from this *position* of buffer
//		int _pos = position;
//
//		// Length of JSON
//		int length = buffer.getInt(_pos);
//
//		// Get JSON string by it`s length
//		// Jump 4 because getInt() == 4 bytes
//		String jsonStr = buffer.getString(_pos += 4, _pos += length);
//		JsonArray contentJson = new JsonArray(jsonStr);
//
//		// We can finally create custom message object
//		return new ConfigurazioneDto(contentJson);
//	}
//
//	@Override
//	public ConfigurazioneDto transform(ConfigurazioneDto conf) {
//		// If a message is sent *locally* across the event bus.
//		// This example sends message just as is
//		return conf;
//	}
//
//	@Override
//	public String name() {
//		// Each codec must have a unique name.
//		// This is used to identify a codec when sending a message and for unregistering codecs.
//		return this.getClass().getSimpleName();
//	}
//
//	@Override
//	public byte systemCodecID() {
//		// Always -1
//		return -1;
//	}

}
