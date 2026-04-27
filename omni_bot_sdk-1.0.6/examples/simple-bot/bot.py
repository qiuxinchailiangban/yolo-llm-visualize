import os
import sys

sys.path.insert(0, os.path.abspath(os.getcwd()))

try:
    from omni_bot_sdk.bot import Bot
except ImportError as e:
    print(f"ImportError: {e}")
    sys.exit(1)


def main():
    bot = Bot(config_path="./config.yaml")
    bot.start()


if __name__ == "__main__":
    main()
