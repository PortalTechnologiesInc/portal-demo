<script>
  import { loggedIn, profile, sessionToken, pubkey } from '../state.svelte.js';

  function logout() {
    loggedIn.set(false);
    sessionToken.set(null);
    profile.set(null);
    pubkey.set(null);
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
    <label class="uk-form-label" for="pubkey">Public Key</label>
    <input
      class="uk-input"
      id="pubkey"
      type="text"
      value="{$pubkey}"
      disabled
    />
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
