<script>
  import Theme from './lib/Theme.svelte'
  import Login from './lib/Login.svelte'
  import { count } from './state.svelte.js';
  import { onMount } from 'svelte';
  import { connected } from './socket.svelte.js';
  onMount(() => {
    count.set(245);

    alert(connected);
  })
</script>


<Login />