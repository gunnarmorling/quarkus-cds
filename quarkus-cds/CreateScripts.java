import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CreateScripts {
    public static void main(String... args) throws Exception {
        String runnerJar = getRunnerJar();
        String classPath = runnerJar;
        
        try (Stream<Path> walk = Files.walk(new File(".").toPath().resolve("target").resolve("lib"))) {

            classPath = classPath + ":" + walk.filter(Files::isRegularFile)
                    .map(x -> "target/lib/" + x.getFileName().toString())
                    .collect(Collectors.joining(":"));
        }

        String patch = new StringBuilder("zip -d " + runnerJar + " META-INF/MANIFEST.MF")
            .append("\njar -uvfm " + runnerJar + " MANIFEST.MF")
            .toString();

        String prepare = "java --class-path \"" + classPath + "\" -XX:ArchiveClassesAtExit=target/app-cds.jsa io.quarkus.runner.GeneratedMain";
        String run = "java --class-path \"" + classPath + "\" -Xlog:class+load:file=target/classload.log io.quarkus.runner.GeneratedMain";
        String runCds = "java --class-path \"" + classPath + "\" -XX:SharedArchiveFile=target/app-cds.jsa -Xlog:class+load:file=target/classload.log io.quarkus.runner.GeneratedMain";

        Path patchFile = new File(".").toPath().resolve("patch-runner.sh");
        Files.writeString(patchFile, patch);
        Files.setPosixFilePermissions(patchFile, PosixFilePermissions.fromString("rwxr--r--"));

        Path prepareFile = new File(".").toPath().resolve("prepare-cds.sh");
        Files.writeString(prepareFile, prepare);
        Files.setPosixFilePermissions(prepareFile, PosixFilePermissions.fromString("rwxr--r--"));

        Path runFile = new File(".").toPath().resolve("run.sh");
        Files.writeString(runFile, run);
        Files.setPosixFilePermissions(runFile, PosixFilePermissions.fromString("rwxr--r--"));

        Path runCdsFile = new File(".").toPath().resolve("runCds.sh");
        Files.writeString(runCdsFile, runCds);
        Files.setPosixFilePermissions(runCdsFile, PosixFilePermissions.fromString("rwxr--r--"));
    }

    private static String getRunnerJar() throws Exception {
        try (Stream<Path> target = Files.walk(new File(".").toPath().resolve("target"), 1)) {
            return target.filter(n -> n.toString().endsWith("runner.jar"))
                .map(x -> "target/" + x.getFileName().toString())
                .findFirst()
                .get();
        }
    }
}
