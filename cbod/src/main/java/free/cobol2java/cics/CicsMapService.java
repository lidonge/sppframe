package free.cobol2java.cics;

public interface CicsMapService<I, O> {

    CicsReceiveResult<I> receive(String mapName, String mapSetName);

    CicsSendResult send(String mapName, String mapSetName, O output);
}