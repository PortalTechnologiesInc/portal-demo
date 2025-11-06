<script>
import { loggedIn, profile, sessionToken } from '../state.svelte.js';
import { ws } from '../socket.svelte.js';

// check if $k=loggedIn is changed and if so, redirect to the home page
$: if ($loggedIn) {
    let profileName = $profile.name;
    UIkit.notification("<uk-icon icon='rocket'></uk-icon> Welcome back " + profileName + "!", { status: 'success' });
  }


function testToken() {
    ws.send('RequestSinglePayment,' + $sessionToken);
}
function logout() {
    loggedIn.set(false);
    profile.set(null);
    sessionToken.set(null);
}
</script>


<div class="flex min-h-svh items-center justify-center p-4 md:bg-muted md:p-10">
    <div class="w-full max-w-md">
        <div class="mb-6 flex justify-center">
            <a href="#">
              <img
                src="portal-logo-normal.webp"
                alt="Portal logo."
                width="75"
              />
            </a>
          </div>
          <div
            class="fr-widget border-border bg-background text-foreground md:border md:p-6 text-center"
          >
          
            <h1>Hello World</h1>
            <button class="uk-btn uk-btn-default" on:click={testToken}>Test Token</button>
            <button class="uk-btn uk-btn-destructive" on:click={logout}>Logout</button>
          </div>

    </div>
</div>

