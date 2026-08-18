# Local AI Client

Local AI Client is a privacy-first Android app for chatting with locally running LLMs on your own network. It connects to providers such as Ollama and LM Studio, keeps conversations on device, and makes it convenient to create, continue, retry, stop, and manage local AI chats.

## Why It Exists

- Use local AI models from a phone without sending prompts to cloud services.
- Work with providers running on a desktop, laptop, or server in the same LAN.
- Keep chat history in local storage so conversations survive screen exits and process restarts.
- Manage multiple conversations for selected local models from one mobile client.

## Supported Providers

- Ollama through its native local HTTP API.
- LM Studio through its OpenAI-compatible local API.

Provider selection is part of the app flow, so new local providers can be added behind the same domain-level connection and generation contracts.

## Architecture

- `app` owns Android application setup, permissions, WorkManager configuration, and theming.
- `presentation` owns Compose screens, navigation, UI state, and ViewModels.
- `domain` owns provider-neutral models, repositories, and use cases.
- `data` owns Room persistence, provider HTTP clients, DTO mapping, repositories, and background generation scheduling.

The app follows an offline-first chat flow: user and assistant placeholder messages are stored locally first, and background work updates the persisted assistant message after the local provider responds.

## Local Development

1. Start a local provider on a machine reachable from the Android device or emulator.
2. Open the app and select the provider.
3. Enter the host and port for the provider on your local network.
4. Connect, choose a model, and start managing local conversations.

Default provider ports:

- Ollama: `11434`
- LM Studio: `1234`

## Privacy Model

Local AI Client is designed around local network usage. Prompts, responses, and conversation metadata are persisted on device, while generation requests are sent only to the provider host configured by the user.
