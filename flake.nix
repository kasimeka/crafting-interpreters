{
  nixConfig.bash-prompt-prefix = ''\[\e[0;31m\](java) \e[0m'';
  description = "JDK 21 env";

  inputs.flake-utils.url = "github:numtide/flake-utils";

  outputs = {
    self,
    nixpkgs,
    flake-utils,
  }:
    flake-utils.lib.eachDefaultSystem (
      system: let
        pkgs = import nixpkgs {
          inherit system;
          overlays = [(_: _: {inherit jdk jre;})];
        };
        jdk = pkgs.graalvmPackages.graalvm-ce-musl;
        jre = pkgs.graalvmPackages.graalvm-ce-musl;
        graalvmDrv = pkgs.graalvmPackages.graalvm-ce-musl;

        generate-expr-file = pkgs.callPackage ./hack/generate-expr.nix {};

        pname = "jlox";
        version = "0.0.0-dev";
        mainClass = "com.craftinginterpreters.lox.Lox";
        drv = pkgs.buildGraalvmNativeImage {
          inherit pname version graalvmDrv;
          src = "${jar}/share/java/${pname}.jar";
          extraNativeImageBuildArgs = ["--static" "--libc=musl" "-march=native"];
        };
        jar = pkgs.stdenv.mkDerivation {
          inherit pname version;
          src = ./src;

          buildInputs = [jre];
          nativeBuildInputs = [jdk pkgs.stripJavaArchivesHook];

          buildPhase = ''find . -name '*.java' -type f -exec javac -d build/ {} +'';
          installPhase = ''
            (cd build && jar cvfe $out/share/java/${pname}.jar ${mainClass} *)
            mkdir -p $out/bin && cat <<EOF > $out/bin/${pname}
            #!usr/bin/env sh
            JAVA_HOME=${jre} exec ${jre}/bin/java -jar $out/share/java/${pname}.jar "\$@"
            EOF
            chmod +x $out/bin/${pname}
          '';
          meta.mainProgram = pname;
        };
      in {
        packages = {
          inherit generate-expr-file;
          default = self.packages.${system}.native;
          native = drv;
          jvm = jar;
        };

        devShell = pkgs.mkShell {
          inputsFrom = [jar drv];
          packages = with pkgs; [
            java-language-server
            google-java-format
            checkstyle
          ];
          shellHook = ''echo "with love from wrd :)"'';
        };
      }
    );
}
