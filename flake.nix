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

        generate-grammar-classes = (pkgs.callPackage
          ./hack/generate-grammar-classes.nix {}) [
          {
            name = "Expr";
            records = {
              Logical = "Expr left, Token operator, Expr right";
              Binary = "Expr left, Token operator, Expr right";
              Unary = "Token operator, Expr right";
              Grouping = "Expr expression";
              Literal = "Object value";
              If = "Expr condition, Expr first, Expr second";
              Variable = "Token name";
              Assign = "Token name, Expr value";
            };
          }
          {
            name = "Stmt";
            imports = ["java.util.List" "java.util.Optional"];
            records = {
              Block = "List<Stmt> statements, boolean enclosedInLoop";
              Expression = "Expr expression";
              Print = "Expr expression";
              Var = "Token name, Expr initializer";
              If = " Expr condition, Stmt.Block thenBranch, Optional<Stmt.Block> elseBranch";
              While = "Expr condition, Stmt.Block body";
              Break = "";
              Continue = "";
            };
          }
        ];

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
          nativeBuildInputs = [
            jdk
            pkgs.stripJavaArchivesHook
            self.packages.${system}.generate-grammar-classes
          ];

          buildPhase = ''
            install -Dm644 -t src $src/*
            ${pkgs.lib.getExe self.packages.${system}.generate-grammar-classes}
            find src -name '*.java' -type f -exec javac -d build/ {} +
          '';
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
          inherit generate-grammar-classes;
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
