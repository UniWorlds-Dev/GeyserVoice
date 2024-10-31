package io.greitan.mineserv.common.network;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.lang.reflect.Constructor;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.greitan.mineserv.common.utils.BaseLogger;
import io.greitan.mineserv.common.network.Payloads.PacketType;
import io.greitan.mineserv.common.network.Payloads.MCCommPacket;
import io.greitan.mineserv.common.network.Payloads.LoginPacket;
import io.greitan.mineserv.common.network.Payloads.AcceptPacket;
import io.greitan.mineserv.common.network.Payloads.DenyPacket;
import io.greitan.mineserv.common.network.Payloads.BindPacket;
import io.greitan.mineserv.common.network.Payloads.DisconnectParticipantPacket;
import io.greitan.mineserv.common.network.Payloads.SetDefaultSettingsPacket;

public class Network
{
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static BaseLogger Logger;

    private static String Version = "1.0.0";

    public <T extends BaseLogger> Network(T logger)
    {
        this.Logger = logger;
    }

    public MCCommPacket sendPostRequest(String url, MCCommPacket data) // was (String url, Object data)
    {
        try
        {
            String jsonData = objectMapper.writeValueAsString(data);
            Logger.debug("Request: " + jsonData.toString());

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            String body = response.body();

            Logger.debug("Response: " + body);

            if (statusCode == 200)
            {
                return objectMapper.readValue(body, MCCommPacket.class);
            }
            else
            {
                Logger.error("Got non-200 status code: " + statusCode);
                return null;
            }
        }
        catch (Exception e)
        {
            Logger.error("Can't connect to voice chat server! " + e.toString());
            return null;
        }
    }

    /**
     * Sends the login request to the server.
     *
     * @param link HTTP POST link
     * @param serverKey The server key
     * @return Token if connected successfully, otherwise null.
     */
    public String sendLoginRequest(String link, String serverKey)
    {
        // Create request data object.
        LoginPacket loginPacket = new LoginPacket();
        loginPacket.LoginKey = serverKey;
        loginPacket.Version = Version;

        MCCommPacket response = sendPostRequest(link, loginPacket);
        if (response != null)
        {
            if (response.PacketId == PacketType.Accept.ordinal())
            {
                AcceptPacket packetData = objectMapper.convertValue(response, AcceptPacket.class);
                return packetData.Token;
            }
/*
            else if (response.PacketId == PacketType.Deny.ordinal())
            {
                DenyPacket packetData = objectMapper.convertValue(response, DenyPacket.class);
// Login Denied. Server denied link request! Reason:
                return "DENIED! " + packetData.Reason;
            }
*/
        }
        return null;
    }

    /**
     * Sends the bind request to the server.
     *
     * @param link HTTP POST link
     * @param token The token from the login
     * @param playerKey The bind key for the player
     * @param playerId The unique but consistent ID of the player
     * @param playerName The name of the player
     * @return "SUCCESS" if binded successfully, otherwise null or reason for failure.
     */
    public String sendBindRequest(String link, String token, Integer playerKey, String playerId, String playerName)
    {
        // Create request data object.
        BindPacket bindPacket = new BindPacket();
        bindPacket.PlayerId = playerId;
        bindPacket.PlayerKey = playerKey;
        bindPacket.Gamertag = playerName;
        bindPacket.Token = token;

        MCCommPacket bindStatus = sendPostRequest(link, bindPacket);
        if (bindStatus == null) return null;

        if (bindStatus.PacketId == PacketType.Accept.ordinal())
        {
            return "SUCCESS";
        }
        else if (bindStatus instanceof DenyPacket)
        {
            DenyPacket packetData = objectMapper.convertValue(bindStatus, DenyPacket.class);
            Logger.error("Binding " + bindPacket.Gamertag + " to " + playerKey + " failed. Reason: " + packetData.Reason);
            return packetData.Reason;
        }
        return null;
    }

    /**
     * Sends the disconnect request to the server.
     *
     * @param link HTTP POST link
     * @param token The token from the login
     * @param playerId The unique but consistent ID of the player
     * @param playerName The name of the player
     * @return "SUCCESS" if disconnected successfully, otherwise null or reason for failure.
     */
    public String sendDisconnectRequest(String link, String token, String playerId, String playerName)
    {
        // Create request data object.
        DisconnectParticipantPacket disconnectParticipantPacket = new DisconnectParticipantPacket();
        disconnectParticipantPacket.Token = token;
        disconnectParticipantPacket.PlayerId = playerId;

        MCCommPacket disconnectStatus = sendPostRequest(link, disconnectParticipantPacket);
        if (disconnectStatus == null) return null;

        if (disconnectStatus.PacketId == PacketType.Accept.ordinal())
        {
            return "SUCCESS";
        }
        else if (disconnectStatus instanceof DenyPacket)
        {
            DenyPacket packetData = objectMapper.convertValue(disconnectStatus, DenyPacket.class);
            Logger.error("Disconnecting player " + playerName + " failed. Reason: " + packetData.Reason);
            return packetData.Reason;
        }
        return null;
    }

    /**
     * Updates the voice chat settings.
     *
     * @param link HTTP POST link
     * @param token The token from the login
     * @param proximityDistance Proximity distance setting.
     * @param proximityToggle   Proximity toggle setting.
     * @param voiceEffects      Voice effects setting.
     * @return True if settings were updated successfully, otherwise false.
     */
    public Boolean sendUpdateSettingsRequest(String link, String token, int proximityDistance, Boolean proximityToggle, Boolean voiceEffects)
    {
        // Create request data object.
        SetDefaultSettingsPacket setDefaultSettingsPacket = new SetDefaultSettingsPacket();
        setDefaultSettingsPacket.ProximityDistance = proximityDistance;
        setDefaultSettingsPacket.ProximityToggle = proximityToggle;
        setDefaultSettingsPacket.VoiceEffects = voiceEffects;
        setDefaultSettingsPacket.Token = token;

        return sendPostRequest(link, setDefaultSettingsPacket) != null;
    }
}
