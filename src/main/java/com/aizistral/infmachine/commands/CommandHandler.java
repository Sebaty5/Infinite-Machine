package com.aizistral.infmachine.commands;

import com.aizistral.infmachine.InfiniteMachine;
import com.aizistral.infmachine.ZOLDdata.Voting;
import com.aizistral.infmachine.config.InfiniteConfig;
import com.aizistral.infmachine.config.Localization;
import com.aizistral.infmachine.database.DataBaseHandler;
import com.aizistral.infmachine.leaderboard.LeaderBoard;
import com.aizistral.infmachine.utils.StandardLogger;
import com.aizistral.infmachine.utils.Utils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CommandHandler extends ListenerAdapter {
    private static final StandardLogger LOGGER = new StandardLogger("CommandHandler");
    public static final CommandHandler INSTANCE = new CommandHandler();

    private CommandHandler() {
        InfiniteMachine.INSTANCE.getJDA().addEventListener(this);
    }

    public void init() {
        registerCommands();
    }

    private void registerCommands() {
        LOGGER.log("Registering Commands...");
        // TODO Better localization
        InfiniteMachine.INSTANCE.getJDA().updateCommands().addCommands(
                //Test Commands
                Commands.slash("ping", Localization.translate("cmd.ping.desc")),
                Commands.slash("version", Localization.translate("cmd.version.desc")),
                Commands.slash("uptime", Localization.translate("cmd.uptime.desc")),
                //Rating Commands
                Commands.slash("leaderboard", Localization.translate("cmd.leaderboard.desc"))
                        .addOption(OptionType.INTEGER, "start", Localization.translate("cmd.leaderboard.start"), false),
                Commands.slash("rating", Localization.translate("cmd.rating.desc"))
                        .addOption(OptionType.USER, "user", Localization.translate("cmd.rating.user"), false),
                //Fun Commands
                Commands.slash("pet", Localization.translate("cmd.pet.desc"))
                        .addOption(OptionType.USER, "user", Localization.translate("cmd.pet.user"), false),
                //ToDo Add option to pet role
                //Admin Commands
                Commands.slash("terminate", Localization.translate("cmd.terminate.desc"))
                        .setDefaultPermissions(DefaultMemberPermissions.DISABLED),
                Commands.slash("fullindex", Localization.translate("cmd.fullindex.desc"))
                        .setDefaultPermissions(DefaultMemberPermissions.DISABLED),
                Commands.slash("openvoting", Localization.translate("cmd.openvoting.desc"))
                        .addOption(OptionType.USER, "user", Localization.translate("cmd.openvoting.user"), true)
                        .addOption(OptionType.STRING, "type", Localization.translate("cmd.openvoting.type", Arrays.stream(Voting.Type.values()).map(Voting.Type::toString).collect(Collectors.joining("/"))), false)
                        .setDefaultPermissions(DefaultMemberPermissions.DISABLED),
                Commands.slash("sendmessage", Localization.translate("cmd.sendmessage.desc"))
                        .addOption(OptionType.CHANNEL, "channel", Localization.translate("cmd.sendmessage.channel"), true)
                        .addOption(OptionType.STRING, "message", Localization.translate("cmd.sendmessage.message"), true)
                        .setDefaultPermissions(DefaultMemberPermissions.DISABLED)
        ).queue();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        LOGGER.log("SlashCommandInteraction received");

        if (event.getChannel().getIdLong() != InfiniteConfig.INSTANCE.getMachineChannelID() && event.getChannel().getIdLong() != InfiniteConfig.INSTANCE.getTempleChannelID() && !"pet".equals(event.getName())) {
            event.reply("It is the wrong place and time...").queue();
            return;
        }

        switch (event.getName()) {
            case "ping": {
                ping(event);
                break;
            }
            case "version": {
                event.reply(String.format("The machine's version is: **%s**", DataBaseHandler.INSTANCE.retrieveInfiniteVersion())).queue();
                break;
            }
            case "uptime": {
                uptime(event);
                break;
            }
            case "leaderboard": {
                event.deferReply().flatMap(v -> LeaderBoard.INSTANCE.getLeaderboardString(event)).queue();
                break;
            }
            case "rating": {
                event.deferReply().flatMap(v -> LeaderBoard.INSTANCE.getRatingString(event)).queue();
                break;
            }
            case "pet": {
                pet(event);
                break;
            }
            case "terminate": {
                InfiniteMachine.INSTANCE.shutdown();
                break;
            }
            case "fullindex": {
                ;
                break;
            }
            case "openvoting": {
                pet(event);
                break;
            }
            case "sendmessage": {
                pet(event);
                break;
            }
            default: {
                event.reply("Hmm strange this command appears to be not implemented yet. Perhaps ask the Infinite Technician about it?").queue();
            }
        }

/*
            if ("rating".equals(event.getName())) {
                event.deferReply().flatMap(v -> {
                    return getRatingString(event);
                }).queue();
            } else if ("clearindex".equals(event.getName())) {
                event.deferReply().flatMap(v -> {
                    this.getDatabase().resetIndexation();
                    this.getDatabase().forceSave();
                    return event.getHook().sendMessage(Localization.translate("msg.indexReset"));
                }).queue();
            } else if ("openvoting".equals(event.getName())) {
                event.deferReply().setEphemeral(true).queue();

                User user = event.getOption("user").getAsUser();
                OptionMapping typeMapping = event.getOption("type");
                Voting.Type type = typeMapping != null ? Voting.Type.valueOf(typeMapping.getAsString())
                        : Voting.Type.GRANT_ROLE;

                this.getDatabase().addMessageCount(user.getIdLong(), 1);
                this.votingHandler.openVoting(user, -1, false, type).thenAccept(voting -> {
                    if (voting == null) {
                        event.getHook().sendMessage(Localization.translate("msg.openVotingFail", user.getId()))
                                .queue();
                    } else {
                        event.getHook().sendMessage(Localization.translate("msg.openVotingSuccess", user.getId()))
                                .queue();
                    }
                });
            } else if ("version".equals(event.getName())) {
                event.reply(String.format("The machine's version is: **%s**", this.getVersion())).queue();
            } else if ("pet".equals(event.getName())) {

            } else if ("sendmessage".equals(event.getName())) {
                OptionMapping channelMapping = event.getOption("channel");
                OptionMapping messageMapping = event.getOption("message");

                long channelID = channelMapping.getAsChannel().getIdLong();
                String message = messageMapping.getAsString();

                Channel targetChannel = jda.getGuildChannelById(channelID);
                if (!(targetChannel instanceof TextChannel)) {
                    event.reply("The specified channel is not a valid text channel.").queue();
                } else {
                    ((TextChannel) targetChannel).sendMessage(message).queue(v -> {
                        event.reply("Message as been send.").queue();
                    });
                }
            }*/
    }

    private static void uptime (SlashCommandInteractionEvent event){
        long uptime = System.currentTimeMillis() - InfiniteMachine.INSTANCE.getStartupTime();

        long hrs = TimeUnit.MILLISECONDS.toHours(uptime);
        uptime -= hrs * 60L * 60L * 1000L;
        long mins = TimeUnit.MILLISECONDS.toMinutes(uptime);
        uptime -= mins * 60L * 1000L;
        long secs = TimeUnit.MILLISECONDS.toSeconds(uptime);

        event.reply(Localization.translate("msg.uptime", hrs, mins, secs)).queue();
    }

    private static void ping (SlashCommandInteractionEvent event){
        long time = System.currentTimeMillis();
        event.reply("Pong!").setEphemeral(false).flatMap(v -> event.getHook().editOriginalFormat(
                "Pong: %d ms", System.currentTimeMillis() - time)).queue();
    }

    private void pet(SlashCommandInteractionEvent event) {
        OptionMapping mapping = event.getOption("user");
        long targetID = mapping != null ? mapping.getAsUser().getIdLong() : 310848622642069504L;
        Member author = event.getMember();
        String msg = "<@%s> has been pet <a:pat_pat_pat:1211592019680694272>";

        //Excluding Arkadiy from normal petting rules
        if (targetID == 440381346339094539L) {
            if (Utils.hasRole(author, InfiniteConfig.INSTANCE.getPetmasterRoleID())) {
                msg = petMasterPetMessage(targetID);
                msg += String.format("\nYes %s can pet anyone and anything.", Utils.getRoleByID(InfiniteConfig.INSTANCE.getPetmasterRoleID()));
            } else {
                msg = "You should know, that a soul can't be `/pet`\nNo matter what machines you wield...";
            }
        } else if (Utils.hasRole(author, InfiniteConfig.INSTANCE.getPetmasterRoleID())) {
            msg = petMasterPetMessage(targetID);
        }else if (targetID == 814542724010213427L) {
            if (!Utils.hasRole(author, InfiniteConfig.INSTANCE.getArchitectRoleID())) {
                msg = String.format("How dare you filthy animal lay hands on the muffin of %s.", Utils.getRoleByID(InfiniteConfig.INSTANCE.getArchitectRoleID()));
            }
        }  else if (targetID == 1124053065109098708L) { // bot's own ID
            msg = "At the end of times, the <@%s> has pet itself <a:pat_pat_pat:1211592019680694272>";
        }
        event.reply(String.format(msg, targetID)).queue();
    }

    private String petMasterPetMessage(long targetId) {
        Random rand = new Random();
        long petmasterID = InfiniteConfig.INSTANCE.getPetmasterRoleID();
        ArrayList<String> possibleReactions = new ArrayList<>();
        possibleReactions.add(String.format("In a display of wholesome benevolence, <@%s> has been expertly pet by the skilled hands of %s.", targetId, Utils.getRoleByID(petmasterID)));
        possibleReactions.add(String.format("<@%s> has experienced the gentle touch of a master petter, their contentment evident in every stroke.", targetId));
        possibleReactions.add(String.format("<@%s> has experienced the gentle touch of a master petter, their spirit uplifted by the soothing strokes.", targetId));
        possibleReactions.add(String.format("With the utmost precision, <@%s> has been pet to perfection, each gesture a testament of their importance.", targetId));
        possibleReactions.add(String.format("Under the skilled hands of %s, <@%s> has received the most exquisite pets imaginable.", Utils.getRoleByID(petmasterID), targetId));
        possibleReactions.add(String.format("With unparalleled finesse, <@%s> has been graced with the touch of a true petting virtuoso.", targetId));
        possibleReactions.add(String.format("<@%s> has been honored with the tender ministrations of %s, a moment of pure bliss.", targetId, Utils.getRoleByID(petmasterID)));
        possibleReactions.add(String.format("Under the practiced hand of %s, <@%s> has enjoyed masterful petting, finding comfort in each gentle caress.", Utils.getRoleByID(petmasterID), targetId));
        possibleReactions.add(String.format("With each stroke, <@%s> has been reminded of the power of masterful pets.", targetId));
        possibleReactions.add(String.format("<@%s> has been graced with masterful petting, each stroke a symphony of comfort and care.", targetId));
        possibleReactions.add(String.format("In a moment of pure bliss, <@%s> has enjoyed the art of masterful petting, their worries melting away with each touch.", targetId));
        possibleReactions.add(String.format("With each stroke, <@%s> has been transported to a world of serenity, the masterful petting a source of joy and comfort.", targetId));
        possibleReactions.add(String.format("Under the watchful eye of %s, <@%s> has relished in the art of masterful petting, finding renewal and refreshment in the simple act.", Utils.getRoleByID(petmasterID), targetId));
        return possibleReactions.get(Math.abs(rand.nextInt() % possibleReactions.size()));
    }
}