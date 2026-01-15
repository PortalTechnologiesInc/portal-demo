<script>
  import { loggedIn, profile, sessionToken, pubkey } from '../state.svelte.js';

  const uiKit =
    typeof window !== 'undefined'
      ? /** @type {any} */ (Reflect.get(window, 'UIkit'))
      : undefined;

  function logout() {
    loggedIn.set(false);
    sessionToken.set(null);
    profile.set(null);
    pubkey.set(null);
  }

  async function copyPublicKey() {
    if (!$pubkey) return;
    
    try {
      await navigator.clipboard.writeText($pubkey);
      uiKit?.notification('Public key copied to clipboard!');
    } catch (err) {
      // Fallback for older browsers
      const textArea = document.createElement('textarea');
      textArea.value = $pubkey;
      textArea.style.position = 'fixed';
      textArea.style.opacity = '0';
      document.body.appendChild(textArea);
      textArea.select();
      try {
        document.execCommand('copy');
        uiKit?.notification('Public key copied to clipboard!');
      } catch (err) {
        uiKit?.notification('Failed to copy public key', { status: 'danger' });
      }
      document.body.removeChild(textArea);
    }
  }
</script>

<div class="space-y-6">
  <div>
    <h3 class="text-lg font-medium">Profile</h3>
    <p class="text-muted-foreground text-sm">
      This is how others will see you on the site.
    </p>
  </div>
  <div class="border-border border-t"></div>
  <div class="space-y-2">
    <label class="uk-form-label" for="pubkey">Public Key (hex)</label>
    <div class="flex gap-2">
      <input
        class="uk-input flex-1"
        id="pubkey"
        type="text"
        value="{$pubkey}"
        disabled
      />
      <button
        class="uk-btn uk-btn-default"
        type="button"
        on:click={copyPublicKey}
        title="Copy public key"
      >
        <uk-icon icon="copy"></uk-icon>
      </button>
    </div>
    <div class="uk-form-help text-muted-foreground">
      This is your public key. It is used to identify you on the network.
    </div>
  </div>         
  <div class="space-y-2">
    <label class="uk-form-label" for="name">Name</label>
    <input
      class="uk-input"
      id="name"
      type="text"
      value="{$profile.name}"
      disabled
    />
    <div class="uk-form-help text-muted-foreground">
      This is your public display name. It can be your real name or a
      pseudonym.
    </div>
  </div>
  <div class="space-y-2">
    <label class="uk-form-label" for="email">Display Name</label>
    <input
      class="uk-input"
      id="displayName"
      type="text"
      value="{$profile.displayName}"
      disabled
    />
    <div class="uk-form-help text-muted-foreground">
      This is your display name.
    </div>
  </div>
  <div class="space-y-2">
    <label class="uk-form-label" for="nip05">Nip05</label>
    <input
      class="uk-input"
      id="nip05"
      type="text"
      value="{$profile.nip05}"
      disabled
    />
    <div class="uk-form-help text-muted-foreground">
      This is your Nip05 address.
    </div>
  </div>
  <div class="">
    <button class="uk-btn uk-btn-primary" on:click={logout}>Logout</button>
  </div>
</div>
