package advancedfactorymanager.reference;

import cpw.mods.fml.common.ModMetadata;

import java.util.Arrays;

public class Metadata
{
    public static ModMetadata init(ModMetadata metadata)
    {
        metadata.modId = Reference.ID;
        metadata.name = Reference.NAME;
        metadata.description = "Advanced Factory manager is a forked and updated version of Vswe's Steve's Factory Manager mod";
        metadata.url = "https://github.com/hilburn/AdvancedFactoryManager";
        metadata.version = Reference.VERSION_FULL;
        metadata.authorList = Arrays.asList("hilburn");
        metadata.credits = "Credit for the original mod goes to Vswe";
        metadata.autogenerated = false;
        return metadata;
    }
}
