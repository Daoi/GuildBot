import net.dv8tion.jda.client.entities.Application;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.requests.RestAction;
import javax.security.auth.login.LoginException;
import java.io.*;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


public class Main extends ListenerAdapter {
    static String schedule = "";
    static final File file = new File(System.getProperty("user.dir"), "schedule.txt");
    static final String path = file.getPath();
    static final Set<String> wowClasses = new HashSet<String>(Arrays.asList(new String[] {"PALADIN", "MAGE", "DRUID","ROGUE","WARRIOR","WARLOCK","PRIEST","HUNTER"}));




    public static void main(String[] args) throws LoginException {
        JDA api = new JDABuilder("censored api key").build();
        api.addEventListener(new Main());
        updateSchedule();

    }
        //On server join
        @Override
        public void onGuildMemberJoin(GuildMemberJoinEvent event){
            try {
                Member member = event.getMember();
                User user = member.getUser();
                String name = member.getUser().getName();
                user.openPrivateChannel().queue((channel) -> channel.sendMessage("Welcome " + name + ". To apply to the guild, check out the sign-up channel; Read it fully." +
                        "Otherwise feel free to hang out in our default channel.").queue());
            }
            catch(Throwable t){
                t.printStackTrace();
            }

        }



        //Bot Commands and Application Handling
        @Override
        public void onMessageReceived(MessageReceivedEvent event)
        {

            Message message = event.getMessage();
            try{
                if(event.getChannel().getId().equalsIgnoreCase("561750053237293078")) return; //Ignore entry hall
                if (event.getAuthor().isBot()) return; //Don't talk to other bots


                //Command Handling
            if(event.getMessage().getContentRaw().charAt(0) == '!' ){
                String[] args = message.getContentRaw().split(" ",2);
                    //SET SCHEDULE START
                    if(args[0].equalsIgnoreCase("!ss") && event.getMember().getRoles().toString().contains("Officer")) {
                    System.out.println("Reached");
                        if (args.length == 1) { //Avoid out of bounds
                            event.getChannel().sendMessage("Must put text after this command").queue();
                            return;
                        }
                    try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), "utf-8"))) {
                        event.getChannel().sendMessage("Schedule updated.").queue();
                        writer.write(args[1]);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                        updateSchedule();
                }   //SCHEDULE DISPLAY START
                    else if(args[0].equalsIgnoreCase("!schedule")){
                        event.getChannel().sendMessage(schedule).queue();
                }
                    else if(args[0].equalsIgnoreCase("!help")){
                        event.getMember().getUser().openPrivateChannel().queue((channel) -> channel.sendMessage("Bot commands are as follows: \n " +
                                "!Schedule - Display the current schedule \n " +
                                "!ss text - Update the schedule with text, officers only \n"+
                                "!class Class:Role:Profession1:Profession2:ExpectedAvailability:Commnets - e.g. \"!class Paladin:Healer:Enchanter:Engineer:Full:This is a comment\" (Quotes not needed)").queue());
                    }//CLASS ROSTER UPDATE START
                    else if(args[0].equalsIgnoreCase("!class" ) && event.getMember().getRoles().toString().contains("Member")){

                        if(args.length == 1)
                            return;


                        //Make it an array list so if a field is empty its not a problem
                        ArrayList<String> classUpdate = new ArrayList<>(Arrays.asList(args[1].split(":")));

                        if(classUpdate.size() < 6) {
                            for (int i = 6 - classUpdate.size(); i == 6; i++){
                                classUpdate.add("N/A");}
                        }
                        //Convert array list to string array because its easier to work with in the sheets api
                        String[] inputArray = new String[6];
                        int count = 0;
                        for (String s:classUpdate) {
                            inputArray[count] = s;
                            count++;
                        }

                        String[] name = new String[1];
                        name[0] = event.getMember().getUser().getName();

                        if(!wowClasses.contains(inputArray[0].toUpperCase())){
                            event.getMember().getUser().openPrivateChannel().queue((channel) -> channel.sendMessage("Invalid class or format, command should look like: " +
                                    "\"!class Paladin:Healer:Enchanter:Engineer:Full:This is a comment\" (Quotes not needed).").queue());
                            return;
                        }

                        RosterHandler.updateRoster(inputArray, name);

                        event.getGuild().getController().addRolesToMember(event.getMember(),event.getJDA().getRolesByName(inputArray[0], true)).complete();

                        if(event.getMember().getRoles().toString().contains(inputArray[0])){
                            event.getMember().getUser().openPrivateChannel().queue((channel) -> channel.sendMessage("Class added successfully.").queue());}


                        }
            }//End of Commands
            }
            catch(Throwable t){
                System.out.println("Error with command message?");
            }

            if(event.getChannel().getId().equalsIgnoreCase("562450805995733022") && event.getMessage().getContentRaw().equalsIgnoreCase("Monster Energy Zero Ultra") && event.getMember().getRoles().size() < 1){
                event.getGuild().getController().addRolesToMember(event.getMember(), event.getJDA().getRolesByName("Member", true)).complete();
                event.getMessage().delete().queue();
                event.getMember().getUser().openPrivateChannel().queue((channel) -> channel.sendMessage(
                        "You have successfully joined. Please add your planned class choice to the roster by typing in general chat " +
                        "!class Class:Role:Profession1:Profession2:ExpectedAvailability:Comment e.g. " +
                        "\"!class Paladin:Healer:Enchanter:Engineer:After 7pm PST:This is a comment\" (Quotes not needed, Roles include DPS/Healer/Tank)").queue());
                if(event.getMember().getRoles().toString().contains("Member")){
                    event.getGuild().getTextChannelById("562550637087293440").sendMessage(event.getMember().getUser().getAsTag() + " has successfully signed up.").queue();
                }
            }





            //Application handling
            /*try {
               if (event.isFromType(ChannelType.PRIVATE)) {
                   if (event.getMessage().getContentRaw().contains("!application")) {

                       User author = event.getAuthor();
                       TextChannel channel = event.getAuthor().getMutualGuilds().get(0).getTextChannelById("562002298033930240");
                       String content = message.getContentDisplay().substring(12);
                       channel.sendMessage(author.getAsTag() + " " + content).queue();
                   }
               }
           }
            catch (Throwable t)
            {
                event.getChannel().sendMessage("An internal error occurred and I wasn't able to process that command. Please ask someone to investigate").complete();
                t.printStackTrace();
            }*/

            //Sign Ups

            }

    public static void updateSchedule(){
        try(BufferedReader reader = new BufferedReader(new FileReader(path))) {
            schedule = "";
            String line;
            while ((line = reader.readLine()) != null)
                schedule = schedule + line + "\n";
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

        }




