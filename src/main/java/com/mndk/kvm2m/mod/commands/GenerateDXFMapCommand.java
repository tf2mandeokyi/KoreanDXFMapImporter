package com.mndk.kvm2m.mod.commands;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import com.mndk.kvm2m.core.vectormap.VectorMapToMinecraftWorld;
import com.mndk.kvm2m.mod.KVectorMap2MinecraftMod;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

public class GenerateDXFMapCommand extends CommandBase {
    @Override
    public String getName() {
        return "gendxfmap";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "gendxfmap <id>";
    }
    
    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
    	return true;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
    	
    	if(!(sender instanceof EntityPlayer)) {
    		throw new CommandException("Only players can run this command.");
    	}
    	
    	if(!(sender instanceof EntityPlayerMP)) {
    		throw new CommandException("This command is only available in the multiplayer server.");
    	}
    	
    	EntityPlayerMP player = (EntityPlayerMP) sender;
    	
        if(args.length == 0) {
            throw new CommandException("com.mndk.kvm2m.cmd.noid");
        }
        try {
        	
        	String subDirectory = args[0];
        	for(int i=1;i<args.length;i++) subDirectory += args[i];
        	File file = new File(KVectorMap2MinecraftMod.dxfFileDirectory + "/" + subDirectory);
        	if(!file.exists()) throw new CommandException("File does not exist!");
        	if(file.isDirectory()) throw new CommandException("File does not exist!");
        	if(!subDirectory.endsWith(".dxf")) throw new CommandException("File does not exist!");
        	
            VectorMapToMinecraftWorld.generate(server, player, file);
            
        } catch(VectorMapToMinecraftWorld.GeneratorException exception) {
            KVectorMap2MinecraftMod.logger.error(exception);
            throw new CommandException(exception.getMessage());
        }
    }
    
    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
    	System.out.println(Arrays.toString(args));
    	if(args.length == 0) {
    		return Arrays.asList(getFilesNDirectoriesInDir("./", ".dxf"));
    	} else {
        	String subDirectory = args[0];
        	for(int i=1;i<args.length;i++) subDirectory += args[i];
    		return Arrays.asList(getFilesNDirectoriesInDir(subDirectory, ".dxf"));
    	}
    }
    
    private static String[] getFilesNDirectoriesInDir(String dir, String extension) {
    	String filePath = KVectorMap2MinecraftMod.dxfFileDirectory + "/" + dir;
    	File file = new File(filePath);
    	final String final_extension = extension.startsWith(".") ? extension : "." + extension;
    	
    	if(!file.exists()) return new String[] {};
    	
    	if(file.isFile()) {
    		return new String[] {};
    	} else /*file.isDirectory()*/ {
    		return Stream.of(file.listFiles())
    				.filter(f -> f.isDirectory() ? true : f.getName().endsWith(final_extension) ? true : false)
    				.map(f -> f.isDirectory() ? f.getName() + "/" : f.getName())
    				.toArray(String[]::new);
    	}
    }
}
