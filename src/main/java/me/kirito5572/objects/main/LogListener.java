package me.kirito5572.objects.main;

import com.amazonaws.SdkClientException;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.guild.voice.GenericGuildVoiceEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceGuildDeafenEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceGuildMuteEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;


public class LogListener extends ListenerAdapter {
    private final static Logger logger = LoggerFactory.getLogger(LogListener.class);
    private final MySqlConnector mySqlConnector;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy년 MM월dd일 HH시mm분ss초");
    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yy/MM/dd a hh:mm:ss");

    public LogListener(MySqlConnector mySqlConnector) {
        this.mySqlConnector = mySqlConnector;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if(!event.isFromGuild()){
            return;
        }
        if (!event.getGuild().getId().equals("826704284003205160")) {
            return;
        }
        boolean isFile = false;

        Message message = event.getMessage();
        if (event.getAuthor().isBot()) {
            return;
        }
        if (message.isWebhookMessage()) {
            return;
        }
        List<Message.Attachment> files = message.getAttachments();
        if (!files.isEmpty()) {
            isFile = true;
        }

        try {
            mySqlConnector.Insert_Query("INSERT INTO blitz_bot.ChattingDataTable (messageId, userId, messageRaw, isFile) VALUES (?, ?, ?, ?)",
                    new int[]{mySqlConnector.STRING, mySqlConnector.STRING, mySqlConnector.STRING, mySqlConnector.BOOLEAN},
                    new String[]{message.getId(), message.getAuthor().getId(), message.getContentRaw(), String.valueOf(isFile)});
        } catch (SQLException sqlException) {
            logger.error(sqlException.getMessage());
        }
        if (isFile) {
            int i = 0;
            for (Message.Attachment attachment : files) {
                if (attachment.isImage() || attachment.isVideo()) {
                    i++;
                    File file = new File(attachment.getFileName() + attachment.getFileExtension());
                    try {
                        attachment.getProxy().downloadToFile(file);
                    } catch (CancellationException e) {
                        logger.error("로그 파일 다운로드에 실패했습니다.");
                        logger.warn(e.getMessage());
                        return;
                    }
                    try {
                        S3UploadObject(file, message.getId() + "_" + i);
                    } catch (SdkClientException e) {
                        logger.error(e.getMessage());
                        e.fillInStackTrace();
                    }
                    boolean isFileDeleted = file.delete();
                    if (!isFileDeleted) {
                        logger.warn("파일 삭제에 실패하였습니다. 재시도 중입니다.");
                        isFileDeleted = file.delete();
                        if (!isFileDeleted) {
                            logger.error("파일 삭제에 실패하였습니다.");
                        } else {
                            logger.info("파일 삭제에 성공했습니다.");
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onMessageUpdate(@NotNull MessageUpdateEvent event) {
        if(!event.isFromGuild()){
            return;
        }
        if(!event.getGuild().getId().equals("826704284003205160")) {
            return;
        }
        if(event.getAuthor().isBot()) {
            return;
        }
        if(event.getMessage().isWebhookMessage()) {
            return;
        }
        EmbedBuilder embedBuilder = EmbedUtils.getDefaultEmbed();
        Date time = new Date();
        try (ResultSet resultSet = mySqlConnector.Select_Query("SELECT * FROM blitz_bot.ChattingDataTable WHERE messageId=?",
                new int[] {mySqlConnector.STRING},
                new String[] {event.getMessageId()})){
            Member member = event.getMember();
            assert member != null;
            embedBuilder.setTitle("수정된 메세지")
                    .setColor(Color.ORANGE)
                    .setDescription("[메세지 이동](" + event.getMessage().getJumpUrl() + ")")
                    .addField("작성 채널", event.getChannel().getAsMention(), false);
            if(resultSet.next()) {
                String pastData = resultSet.getString("messageRaw");
                String nowData = event.getMessage().getContentRaw();
                if(pastData != null) {
                    MessageBuilder(embedBuilder, pastData,"수정전 내용", null);
                    MessageBuilder(embedBuilder, nowData,"수정후 내용", event.getMessageId());
                }
            } else {
                embedBuilder.addField("수정전 내용", "정보 없음", false);
                MessageBuilder(embedBuilder, event.getMessage().getContentRaw(),"수정후 내용", null);
            }
            embedBuilder.addField("수정 시간", timeFormat.format(time), false)
                    .setFooter(member.getNickname(), member.getUser().getAvatarUrl());
            Objects.requireNonNull(event.getGuild().getTextChannelById("829023428019355688")).sendMessageEmbeds(embedBuilder.build()).queue();

            mySqlConnector.Insert_Query("UPDATE blitz_bot.ChattingDataTable SET messageRaw=? WHERE messageId=?",
                    new int[] {mySqlConnector.STRING, mySqlConnector.STRING},
                    new String[] {event.getMessage().getContentRaw(), event.getMessageId()});
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent event) {
        if(!event.isFromGuild()){
            return;
        }
        if(!event.getGuild().getId().equals("826704284003205160")) {
            return;
        }
        EmbedBuilder embedBuilder = EmbedUtils.getDefaultEmbed();
        Date time = new Date();
        try (ResultSet resultSet = mySqlConnector.Select_Query("SELECT * FROM blitz_bot.ChattingDataTable WHERE messageId=?;",
                new int[] {mySqlConnector.STRING}, new String[] {event.getMessageId()})) {
            embedBuilder.setTitle("삭제된 메세지")
                    .setColor(Color.RED);
            boolean isFile = false;
            if(resultSet.next()) {
                isFile = resultSet.getBoolean("isFile");
                Member member = event.getGuild().getMemberById(resultSet.getString("userId"));
                if (member == null) {
                    embedBuilder.setFooter("<@" + resultSet.getString("userId") + ">");
                } else {
                    embedBuilder.setFooter(member.getNickname(), member.getUser().getAvatarUrl());
                }
                if(isFile) {
                    embedBuilder.appendDescription("이미지가 포함된 게시글");
                }
                embedBuilder.addField("작성 채널", event.getChannel().getAsMention(), false);
                String messageRaw = resultSet.getString("messageRaw");
                if(resultSet.getString("messageRaw").isEmpty()) {
                    embedBuilder.addField("삭제된 내용", "내용이 없이 사진만 있는 메세지", false);
                } else {
                    MessageBuilder(embedBuilder, messageRaw,"삭제된 내용", event.getMessageId());
                }
            } else {
                embedBuilder.addField("데이터 없음", "데이터 없음", false);
            }
            embedBuilder.addField("삭제 시간", timeFormat.format(time), false);
            if(isFile) {
                try {
                    File file = S3DownloadObject(event.getMessageId() + "_" + 1);
                    Objects.requireNonNull(event.getGuild().getTextChannelById("829023428019355688")).sendFiles(FileUpload.fromData(file)).queue(message ->
                        embedBuilder.setImage("https://cdn.discordapp.com/attachments/829023428019355688/" +
                                message.getId() + "/" + file.getName()));
                } catch (AmazonS3Exception s3Exception) {
                    logger.info("객체에 없는 파일을 요청했습니다.");
                    embedBuilder.appendDescription("이미지가 포함된 글이나 이미지가 존재하지 않습니다(30일 이상 경과 or 업로드 안된 파일)");
                } catch (IOException e) {
                    logger.error(e.getMessage());
                    e.fillInStackTrace();
                }
            }
            Objects.requireNonNull(event.getGuild().getTextChannelById("829023428019355688")).sendMessageEmbeds(embedBuilder.build()).queue();
            mySqlConnector.Insert_Query("DELETE FROM blitz_bot.ChattingDataTable WHERE messageId=?", new int[] {mySqlConnector.STRING}, new String[] {event.getMessageId()});

        } catch (SQLException e) {
            logger.error(e.getMessage());
            e.fillInStackTrace();
        }
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        if(!event.getGuild().getId().equals("826704284003205160")) {
            return;
        }
        EmbedBuilder embedBuilder = EmbedUtils.getDefaultEmbed();
        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yy/MM/dd a hh:mm:ss");
        embedBuilder.setTitle("신규 유저 접속")
                .setColor(new Color(50, 200, 50))
                .setDescription(simpleDateFormat.format(date))
                .addField("유저명", event.getMember().getEffectiveName(), false)
                .setFooter(event.getMember().getId(), event.getMember().getAvatarUrl());
        Objects.requireNonNull(event.getGuild().getTextChannelById("946362857795248188")).sendMessageEmbeds(embedBuilder.build()).queue();
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        if(!event.getGuild().getId().equals("826704284003205160")) {
            return;
        }
        EmbedBuilder embedBuilder = EmbedUtils.getDefaultEmbed();
        Date date = new Date();
        Member member = event.getMember();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yy/MM/dd a hh:mm:ss");
        embedBuilder.setTitle("유저 서버 나감")
                .setColor(new Color(200, 50, 50))
                .setDescription(simpleDateFormat.format(date));
        if(member != null) {
            embedBuilder.addField("유저명", member.getEffectiveName(), false)
                    .setFooter(member.getId(), member.getAvatarUrl());
        } else {
            embedBuilder.addField("유저명", "데이터 알 수 없음", false)
                    .setFooter(event.getUser().getId());
        }
        Objects.requireNonNull(event.getGuild().getTextChannelById("946362857795248188")).sendMessageEmbeds(embedBuilder.build()).queue();
    }

    @Override
    public void onGuildMemberRoleAdd(@NotNull GuildMemberRoleAddEvent event) {
        if(!event.getGuild().getId().equals("826704284003205160")) {
            return;
        }
        Date date = new Date();
        Member member = event.getMember();
        List<Role> roleList = event.getRoles();
        StringBuilder roleData = new StringBuilder();
        for (Role role : roleList) {
            roleData.append(role.getAsMention());
        }
        EmbedBuilder embedBuilder = EmbedUtils.getDefaultEmbed();
        embedBuilder.setTitle("유저 역할 부여")
                .setColor(new Color(255, 200, 0))
                .setDescription(simpleDateFormat.format(date))
                .addField("유저명", member.getAsMention(), false)
                .addField("부여된 역할", roleData.toString(), false)
                .setFooter(member.getId(), member.getAvatarUrl());
        Objects.requireNonNull(event.getGuild().getTextChannelById("946362857795248188")).sendMessageEmbeds(embedBuilder.build()).queue();
    }

    @Override
    public void onGuildMemberRoleRemove(@NotNull GuildMemberRoleRemoveEvent event) {
        if(!event.getGuild().getId().equals("826704284003205160")) {
            return;
        }
        Date date = new Date();
        Member member = event.getMember();
        List<Role> roleList = event.getRoles();
        StringBuilder roleData = new StringBuilder();
        for (Role role : roleList) {
            roleData.append(role.getAsMention());
        }
        EmbedBuilder embedBuilder = EmbedUtils.getDefaultEmbed();
        embedBuilder.setTitle("유저 역할 삭제")
                .setColor(new Color(102,20,153))
                .setDescription(simpleDateFormat.format(date))
                .addField("유저명", member.getAsMention(), false)
                .addField("삭제된 역할", roleData.toString(), false)
                .setFooter(member.getId(), member.getAvatarUrl());
        Objects.requireNonNull(event.getGuild().getTextChannelById("946362857795248188")).sendMessageEmbeds(embedBuilder.build()).queue();
    }

    @Override
    public void onGuildBan(@NotNull GuildBanEvent event) {
        if(!event.getGuild().getId().equals("826704284003205160")) {
            return;
        }
        Date date = new Date();
        User user = event.getUser();
        EmbedBuilder embedBuilder = EmbedUtils.getDefaultEmbed();
        embedBuilder.setTitle("유저 차단 실행")
                .setColor(new Color(200, 50, 50))
                .setDescription(simpleDateFormat.format(date))
                .addField("유저명", user.getAsMention(), false)
                .setFooter(user.getId(), user.getAvatarUrl());
        Objects.requireNonNull(event.getGuild().getTextChannelById("946362857795248188")).sendMessageEmbeds(embedBuilder.build()).queue();
    }

    @Override
    public void onGuildUnban(@NotNull GuildUnbanEvent event) {
        if(!event.getGuild().getId().equals("826704284003205160")) {
            return;
        }
        Date date = new Date();
        User user = event.getUser();
        EmbedBuilder embedBuilder = EmbedUtils.getDefaultEmbed();
        embedBuilder.setTitle("유저 차단 해제")
                .setColor(new Color(50, 200, 50))
                .setDescription(simpleDateFormat.format(date))
                .addField("유저명", user.getAsMention(), false)
                .setFooter(user.getId(), user.getAvatarUrl());
        Objects.requireNonNull(event.getGuild().getTextChannelById("946362857795248188")).sendMessageEmbeds(embedBuilder.build()).queue();
    }

    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        if(!event.getGuild().getId().equals("826704284003205160")) {
            return;
        }
        AudioChannelUnion channelJoined = event.getChannelJoined();
        AudioChannelUnion channelLeft= event.getChannelLeft();

        if(channelJoined != null && channelLeft != null) {
            //채널 이동
            Date date = new Date();
            Member member = event.getMember();
            EmbedBuilder embedBuilder = EmbedUtils.getDefaultEmbed();
            embedBuilder.setTitle("유저 보이스 채널 이동")
                    .setColor(new Color(255, 255, 0))
                    .setDescription(simpleDateFormat.format(date))
                    .addField("이전 채널명", channelLeft.getAsMention(), false)
                    .addField("현재 채널명", channelJoined.getAsMention(), false)
                    .addField("유저명", member.getAsMention(), false)
                    .setFooter(member.getId(), member.getAvatarUrl());
            Objects.requireNonNull(event.getGuild().getTextChannelById("1046784597326835813")).sendMessageEmbeds(embedBuilder.build()).queue();

        } else if(channelJoined != null) {
            //채널 입장
            Date date = new Date();
            Member member = event.getMember();
            EmbedBuilder embedBuilder = EmbedUtils.getDefaultEmbed();
            embedBuilder.setTitle("유저 보이스 채널 입장")
                    .setColor(new Color(50, 200, 50))
                    .setDescription(simpleDateFormat.format(date))
                    .addField("채널명", channelJoined.getAsMention(), false)
                    .addField("유저명", member.getAsMention(), false)
                    .setFooter(member.getId(), member.getAvatarUrl());
            Objects.requireNonNull(event.getGuild().getTextChannelById("1046784597326835813")).sendMessageEmbeds(embedBuilder.build()).queue();

        } else if(channelLeft != null) {
            //채널 퇴장
            Date date = new Date();
            Member member = event.getMember();
            EmbedBuilder embedBuilder = EmbedUtils.getDefaultEmbed();
            embedBuilder.setTitle("유저 보이스 채널 퇴장")
                    .setColor(new Color(200, 50, 50))
                    .setDescription(simpleDateFormat.format(date))
                    .addField("채널명", channelLeft.getAsMention(), false)
                    .addField("유저명", member.getAsMention(), false)
                    .setFooter(member.getId(), member.getAvatarUrl());
            Objects.requireNonNull(event.getGuild().getTextChannelById("1046784597326835813")).sendMessageEmbeds(embedBuilder.build()).queue();
        }
    }

    @Override
    public void onGuildVoiceGuildMute(@NotNull GuildVoiceGuildMuteEvent event) {
        if(!event.getGuild().getId().equals("826704284003205160")) {
            return;
        }
        Date date = new Date();
        Member member = event.getMember();
        EmbedBuilder embedBuilder = EmbedUtils.getDefaultEmbed();
        if(event.isGuildMuted())
            embedBuilder.setTitle("유저 보이스 서버 뮤트")
                    .setColor(new Color(200, 50, 50))
                    .setDescription(simpleDateFormat.format(date))
                    .addField("유저명", member.getAsMention(), false)
                    .setFooter(member.getId(), member.getAvatarUrl());
        else
            embedBuilder.setTitle("유저 보이스 서버 뮤트 해제")
                    .setColor(new Color(50, 200, 50))
                    .setDescription(simpleDateFormat.format(date))
                    .addField("유저명", member.getAsMention(), false)
                    .setFooter(member.getId(), member.getAvatarUrl());
        Objects.requireNonNull(event.getGuild().getTextChannelById("1046784597326835813")).sendMessageEmbeds(embedBuilder.build()).queue();
    }

    @Override
    public void onGuildVoiceGuildDeafen(@NotNull GuildVoiceGuildDeafenEvent event) {
        if(!event.getGuild().getId().equals("826704284003205160")) {
            return;
        }
        Date date = new Date();
        Member member = event.getMember();
        EmbedBuilder embedBuilder = EmbedUtils.getDefaultEmbed();
        if(event.isGuildDeafened())
            embedBuilder.setTitle("유저 보이스 서버 음소거")
                    .setColor(new Color(200, 50, 50))
                    .setDescription(simpleDateFormat.format(date))
                    .addField("유저명", member.getAsMention(), false)
                    .setFooter(member.getId(), member.getAvatarUrl());
        else
            embedBuilder.setTitle("유저 보이스 서버 음소거 해제")
                    .setColor(new Color(50, 200, 50))
                    .setDescription(simpleDateFormat.format(date))
                    .addField("유저명", member.getAsMention(), false)
                    .setFooter(member.getId(), member.getAvatarUrl());
        Objects.requireNonNull(event.getGuild().getTextChannelById("1046784597326835813")).sendMessageEmbeds(embedBuilder.build()).queue();
    }


    /**
     * upload file to bot s3 cloud
     *
     * @param file the {@link File} to upload
     * @param messageId message id of the file to be uploaded
     */

    private void S3UploadObject(@NotNull File file,@NotNull String messageId) throws SdkClientException{
        Regions clientRegion = Regions.AP_NORTHEAST_2;
        String bucketName = "blitzbot-logger";

        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withRegion(clientRegion)
                .withCredentials(new EnvironmentVariableCredentialsProvider())
                .build();

        PutObjectRequest request = new PutObjectRequest(bucketName, messageId, file);
        ObjectMetadata metadata = new ObjectMetadata();
        request.setMetadata(metadata);
        request.setStorageClass(StorageClass.StandardInfrequentAccess);
        s3Client.putObject(request);
    }

    /**
     * download {@link File} to bot s3 cloud
     *
     * @param messageId Message id of the file to be downloaded
     *
     * @return download {@link File} or null(If the file does not exist)
     */

    private @NotNull File S3DownloadObject(@NotNull String messageId) throws SdkClientException, IOException{
        Regions clientRegion = Regions.AP_NORTHEAST_2;
        String bucketName = "blitzbot-logger";

        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withRegion(clientRegion)
                .withCredentials(new EnvironmentVariableCredentialsProvider())
                .build();

        GetObjectRequest request = new GetObjectRequest(bucketName, messageId);
        S3Object object = s3Client.getObject(request);
        ObjectMetadata metadata = object.getObjectMetadata();
        InputStream inputStream = object.getObjectContent();
        Path path = Files.createTempFile(messageId, "." + metadata.getContentType().split("/")[1]);
        FileOutputStream out = new FileOutputStream(path.toFile());
        byte[] buffer = new byte[1024];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
        out.close();
        return path.toFile();
    }

    /**
     * Message data splitter(if Message data's length > 1000, split it)
     *
     * @param embedBuilder the EmbedBuilder which is use addField with split data
     * @param data the message string to be split
     * @param messageId the messageId which is modifying message's id
     *                  input null if the message which is before modification is null
     * @param name the name
     */

    private void MessageBuilder(@NotNull EmbedBuilder embedBuilder, @NotNull String data, @NotNull String name, @Nullable String messageId) {
        if(1000 < data.length() && data.length() <= 2000) {
            embedBuilder.addField("정보", "1000글자를 넘어가는 메세지로 확인되어 여러단락으로 분리했습니다.", false)
                    .addField(name + "-1", data.substring(0, 1000), false)
                    .addField(name + "-2", data.substring(1000), false);
        } else if(2000 < data.length() && data.length() <= 3000) {
            embedBuilder.addField("정보", "1000글자를 넘어가는 메세지로 확인되어 여러단락으로 분리했습니다.", false)
                    .addField(name + "-1", data.substring(0, 1000), false)
                    .addField(name + "-2", data.substring(1000, 2000), false)
                    .addField(name + "-3", data.substring(2000), false);
        } else if(3000 < data.length() && data.length() <= 4000) {
            embedBuilder.addField("정보", "1000글자를 넘어가는 메세지로 확인되어 여러단락으로 분리했습니다.", false)
                    .addField(name + "-1", data.substring(0, 1000), false)
                    .addField(name + "-2", data.substring(1000, 2000), false)
                    .addField(name + "-3", data.substring(2000, 3000), false)
                    .addField(name + "-4", data.substring(3000), false);
        } else {
            embedBuilder.addField(name, data, false);
        }

        if(messageId != null) {
            embedBuilder.addField("메세지 ID", messageId, false);
        }

    }
}
