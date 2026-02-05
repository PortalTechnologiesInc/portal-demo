{
  config,
  lib,
  pkgs,
  ...
}:

with lib;

let
  cfg = config.services.portal-demo-backend;
in
{
  options.services.portal-demo-backend = {
    enable = mkEnableOption "Portal demo backend";

    package = mkOption {
      type = types.package;
      default = pkgs.portal-demo-backend;
      description = "The portal-demo package to use.";
    };

    authToken = mkOption {
      type = types.str;
      description = "Auth token to use for the backend";
    };

    stateDir = mkOption {
      type = types.str;
      default = "/var/lib/portal-demo-backend";
      description = "State directory for portal-backend";
    };

    user = mkOption {
      type = types.str;
      default = "portal-backend";
      description = "User account under which portal-backend runs";
    };

    group = mkOption {
      type = types.str;
      default = "portal-backend";
      description = "Group account under which portal-backend runs";
    };

    restWsEndpoint = mkOption {
      type = types.str;
      default = "ws://localhost:3000/ws";
      description = "REST websocket endpoint to use for the backend";
    };

    databasePath = mkOption {
      type = types.str;
      default = "${cfg.stateDir}/data.db";
      description = "Path to the database file";
    };
  };

  config =
    let
      # Combine all environment variables
      envConfig = {
        REST_TOKEN = cfg.authToken;
        # REST_HEALTH_ENDPOINT = cfg.restHealthEndpoint;
        REST_WS_ENDPOINT = cfg.restWsEndpoint;
        DB_PATH = cfg.databasePath;
      };
    in
    mkIf cfg.enable {
      systemd.services.portal-demo-backend = {
        description = "Portal demo backend";
        wantedBy = [ "multi-user.target" ];
        after = [ "portal-rest.service" ];
        requires = [ "portal-rest.service" ];

        environment = envConfig;

        serviceConfig = {
          ExecStart = "${lib.getExe cfg.package}";
          Restart = "always";
          ProtectSystem = "strict";
          ProtectHome = true;
          PrivateTmp = true;
          NoNewPrivileges = true;
          StateDirectory = "portal-demo-backend";
          User = cfg.user;
          Group = cfg.group;
        };
      };

      systemd.tmpfiles.rules = [
        "d ${cfg.stateDir}                            0700 ${cfg.user} ${cfg.group} - -"
      ];

      services.portal-rest = {
        enable = true;
        authToken = cfg.authToken;
      };

      users.users.${cfg.user} = {
        isSystemUser = true;
        group = cfg.group;
      };
      users.groups.${cfg.group} = {};
    };
}