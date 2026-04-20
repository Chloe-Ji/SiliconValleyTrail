{ pkgs }: {
  deps = [
    # Java 21 + Maven for building and running the game.
    pkgs.jdk21
    pkgs.maven
  ];
}
